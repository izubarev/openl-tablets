package org.openl.rules.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openl.rules.lock.Lock;
import org.openl.rules.lock.LockManager;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.rest.resolver.DesignRepository;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.CreateUpdateProjectModelValidator;
import org.openl.rules.rest.validation.ZipArchiveValidator;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/repos", produces = MediaType.APPLICATION_JSON_VALUE)
public class DesignTimeRepositoryController {

    private final DesignTimeRepository designTimeRepository;
    private final PropertyResolver propertyResolver;
    private final BeanValidationProvider validationProvider;
    private final CreateUpdateProjectModelValidator createUpdateProjectModelValidator;
    private final ZipArchiveValidator zipArchiveValidator;
    private final ZipProjectSaveStrategy zipProjectSaveStrategy;
    private final LockManager lockManager;

    @Autowired
    public DesignTimeRepositoryController(DesignTimeRepository designTimeRepository,
            PropertyResolver propertyResolver,
            BeanValidationProvider validationService,
            CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
            ZipArchiveValidator zipArchiveValidator,
            ZipProjectSaveStrategy zipProjectSaveStrategy,
            @Value("${openl.home.shared}") String homeDirectory) {
        this.designTimeRepository = designTimeRepository;
        this.propertyResolver = propertyResolver;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaveStrategy = zipProjectSaveStrategy;
        this.lockManager = new LockManager(Paths.get(homeDirectory).resolve("locks/api"));
    }

    @GetMapping
    public List<Map<String, Object>> getRepositoryList() {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        return designTimeRepository.getRepositories().stream().map(repository -> {
            Map<String, Object> dest = new LinkedHashMap<>();
            dest.put("id", repository.getId());
            dest.put("name", repository.getName());
            return dest;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{repo-name}/projects")
    public List<Map<String, Object>> getProjectListByRepository(@DesignRepository("repo-name") Repository repository) {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        return designTimeRepository.getProjects(repository.getId())
            .stream()
            .filter(proj -> !proj.isDeleted())
            .sorted(Comparator.comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER))
            .map(src -> mapProjectResponse(src, repository.supports()))
            .collect(Collectors.toList());
    }

    private <T extends AProject> Map<String, Object> mapProjectResponse(T src, Features features) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("name", src.getBusinessName());
        dest.put("modifiedBy",
            Optional.of(src.getFileData()).map(FileData::getAuthor).map(UserInfo::getName).orElse(null));
        dest.put("modifiedAt",
            Optional.of(src.getFileData())
                .map(FileData::getModifiedAt)
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null));
        dest.put("rev", Optional.of(src.getFileData()).map(FileData::getVersion).orElse(null));
        if (features.branches()) {
            dest.put("branch", Optional.of(src.getFileData()).map(FileData::getBranch).orElse(null));
        }
        if (features.mappedFolders()) {
            Optional.ofNullable(src.getRealPath()).ifPresent(p -> dest.put("path", p.replace('\\', '/')));
        }
        return dest;
    }

    @PutMapping(value = "/{repo-name}/projects/{project-name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> createProjectFromZip(@DesignRepository("repo-name") Repository repository,
            @PathVariable("project-name") String projectName,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam("template") MultipartFile file,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") Boolean overwrite) throws IOException {

        SecurityChecker.allow(overwrite ? Privileges.EDIT_PROJECTS : Privileges.CREATE_PROJECTS);
        allowedToPush(repository);

        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repository.getId(),
            getUserName(),
            StringUtils.trimToNull(projectName),
            StringUtils.trimToNull(path),
            StringUtils.isNotBlank(comment) ? comment
                                            : createCommentsService(repository.getId()).createProject(projectName),
            overwrite);
        validationProvider.validate(model); // perform basic validation

        final Path archiveTmp = Files.createTempFile(projectName, ".zip");
        final Lock lock = getLock(repository, model);
        try {
            IOUtils.copyAndClose(file.getInputStream(), Files.newOutputStream(archiveTmp));
            if (!lock.tryLock(getUserName(), 15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Can't create a lock.");
            }
            validationProvider.validate(model, createUpdateProjectModelValidator);
            validationProvider.validate(archiveTmp, zipArchiveValidator);
            FileData data = zipProjectSaveStrategy.save(model, archiveTmp);
            return mapFileDataResponse(data, repository.supports());
        } finally {
            FileUtils.deleteQuietly(archiveTmp);
            lock.unlock();
        }
    }

    private Lock getLock(Repository repository, CreateUpdateProjectModel model) {
        StringBuilder lockId = new StringBuilder(model.getRepoName());
        if (repository.supports().branches()) {
            lockId.append("/[branches]/").append(((BranchRepository) repository).getBaseBranch()).append('/');
        }
        if (repository.supports().mappedFolders() && !StringUtils.isNotEmpty(model.getPath())) {
            lockId.append(model.getPath());
        }
        lockId.append(model.getProjectName());
        return lockManager.getLock(lockId.toString());
    }

    private Comments createCommentsService(String repoName) {
        return new Comments(propertyResolver, repoName);
    }

    private Map<String, Object> mapFileDataResponse(FileData src, Features features) {
        Map<String, Object> dest = new LinkedHashMap<>();
        if (features.branches()) {
            dest.put("branch", src.getBranch());
        }
        dest.put("rev", src.getVersion());
        return dest;
    }

    private String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void allowedToPush(Repository repo) {
        if (repo.supports().branches()) {
            BranchRepository branchRepo = (BranchRepository) repo;
            if (branchRepo.isBranchProtected(branchRepo.getBranch())) {
                throw new ForbiddenException("default.message");
            }
        }
    }
}