package org.openl.rules.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBException;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.project.ProjectStateValidator;
import org.openl.rules.rest.resolver.DesignRepository;
import org.openl.rules.rest.service.ProjectDependencyResolver;
import org.openl.rules.rest.service.ProjectDeploymentService;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentProjectItem;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping(value = "/user-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Workspace")
public class ProjectManagementController {

    private final ProjectDependencyResolver projectDependencyResolver;
    private final ProjectDeploymentService projectDeploymentService;
    private final DeploymentManager deploymentManager;
    private final ProjectStateValidator projectStateValidator;
    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAclService deployConfigRepositoryAclService;

    @Autowired
    public ProjectManagementController(ProjectDependencyResolver projectDependencyResolver,
            ProjectDeploymentService projectDeploymentService,
            DeploymentManager deploymentManager,
            ProjectStateValidator projectStateValidator,
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            @Qualifier("deployConfigRepositoryAclService") RepositoryAclService deployConfigRepositoryAclService) {
        this.projectDependencyResolver = projectDependencyResolver;
        this.projectDeploymentService = projectDeploymentService;
        this.deploymentManager = deploymentManager;
        this.projectStateValidator = projectStateValidator;
        this.designRepositoryAclService = designRepositoryAclService;
        this.deployConfigRepositoryAclService = deployConfigRepositoryAclService;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    /**
     * Returns information about the project and its dependencies.
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @return project info.
     */
    @GetMapping("/{repo-name}/projects/{proj-name}/info")
    @Hidden
    public ProjectInfo getInfo(@DesignRepository("repo-name") Repository repo, @PathVariable("proj-name") String name) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
                throw new SecurityException();
            }
            ProjectInfo info = new ProjectInfo(project);
            info.dependsOn = projectDependencyResolver.getDependsOnProject(project)
                .stream()
                .map(ProjectInfo::new)
                .collect(Collectors.toList());
            info.dependencies = projectDependencyResolver.getProjectDependencies(project)
                .stream()
                .map(ProjectInfo::new)
                .collect(Collectors.toList());
            return info;
        } catch (ProjectException | JAXBException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Returns deployment items for selected project.
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @param deployRepoName name of deploy repository.
     * @return project info.
     */
    @GetMapping("/{repo-name}/projects/{proj-name}/deployments/{deploy-repo-name}")
    @Hidden
    public List<DeploymentProjectItem> getDeploymentItems(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @PathVariable("deploy-repo-name") String deployRepoName) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            return projectDeploymentService.getDeploymentProjectItems(project, deployRepoName);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Closes the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Hidden
    public void close(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            HttpSession session) {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
                throw new SecurityException();
            }
            if (project.isDeleted()) {
                throw new ConflictException("project.close.deleted.message", name);
            } else if (!projectStateValidator.canClose(project)) {
                throw new ConflictException("project.close.conflict.message");
            }
            ProjectHistoryService.deleteHistory(project.getBusinessName());
            // We must release module info because it can hold jars.
            // We cannot rely on studio.getProject() to determine if closing project is compiled inside
            // studio.getModel()
            // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
            // a dependency of other. That's why we must always clear moduleInfo when closing a project.
            webStudio.getModel().clearModuleInfo();
            project.close();
            webStudio.reset();
        } catch (ProjectException | IOException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Opens the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Hidden
    public void open(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam(value = "open-dependencies", required = false, defaultValue = "false") boolean openDependencies,
            HttpSession session) {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
                throw new SecurityException();
            }
            if (project.isDeleted()) {
                throw new ConflictException("project.open.deleted.message", name);
            } else if (!projectStateValidator.canOpen(project)) {
                throw new ConflictException("project.open.conflict.message");
            } else if (getUserWorkspace().isOpenedOtherProject(project)) {
                throw new ConflictException("open.duplicated.project");
            }
            project.open();
            if (openDependencies) {
                openAllDependencies(project);
            }
            // User workspace is changed when the project was opened, so we must refresh it to calc dependencies.
            // reset() should internally refresh workspace.
            webStudio.reset();
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Deploy the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @param deployRepoName repository name where to deploy the project.
     * @param items items to deploy.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Hidden
    public void deploy(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam("deploy-repo-name") String deployRepoName,
            @RequestBody String[] items) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!projectStateValidator.canDeploy(project)) {
                if (project.isDeleted()) {
                    throw new ConflictException("project.deploy.deleted.message");
                }
                throw new ConflictException("project.deploy.conflict.message");
            }
            List<DeploymentProjectItem> deploymentProjectItems = projectDeploymentService
                .getDeploymentProjectItems(project, repo.getId());
            List<ADeploymentProject> deploymentProjectsToDeploy = new ArrayList<>();
            for (String item : items) {
                Optional<DeploymentProjectItem> deploymentProjectItem = deploymentProjectItems.stream()
                    .filter(p -> p.getName().equals(item))
                    .findFirst();
                if (deploymentProjectItem.isPresent() && deploymentProjectItem.get().isCanDeploy()) {
                    ADeploymentProject deploymentProject = projectDeploymentService.update(item, project, repo.getId());
                    if (!deployConfigRepositoryAclService.isGranted(deploymentProject, List.of(AclPermission.DEPLOY))) {
                        throw new SecurityException();
                    }
                    deploymentProjectsToDeploy.add(deploymentProject);
                }
            }
            for (ADeploymentProject deploymentProject : deploymentProjectsToDeploy) {
                deploymentManager.deploy(deploymentProject, deployRepoName);
            }
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * WARNING: Currently it's used only for testing purpose. Should be finalized before using in real life
     *
     * @see org.openl.rules.webstudio.web.repository.RepositoryTreeController#deleteNode()
     */
    @Hidden
    @DeleteMapping(value = "/{repo-name}/projects/{proj-name}/delete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED) // change status after full implementation
    public void delete(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam(value = "comment", required = false) final String comment) {
        // FIXME request body is not allowed for DELETE method.
        // see: https://www.rfc-editor.org/rfc/rfc7231#section-4.3.5
        // A payload within a DELETE request message has no defined semantics; sending a payload body on a DELETE request might cause some existing implementations to reject the request.
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.DELETE))) {
                throw new SecurityException();
            }
            if (!projectStateValidator.canDelete(project)) {
                if (project.getDesignRepository().supports().branches() && project.getVersion() == null && !project
                    .isLocalOnly()) {
                    throw new ConflictException("project.delete.branch.message");
                }
                if (project.isLocked() || project.isLockedByMe()) {
                    throw new ConflictException("project.delete.locked.message");
                }
                throw new ConflictException("project.delete.message");
            }
            // TODO: Project should be closed for all users
            project.delete(comment);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * WARNING: Currently it's used only for testing purpose. Should be finalized before using in real life
     *
     * @see org.openl.rules.webstudio.web.repository.RepositoryTreeController#deleteNode()
     */
    @Hidden
    @DeleteMapping(value = "/{repo-name}/projects/{proj-name}/erase", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED) // change status after full implementation
    public void erase(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam(value = "comment", required = false) final String comment) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.ERASE))) {
                throw new SecurityException();
            }
            if (!projectStateValidator.canErase(project)) {
                throw new ConflictException("project.erase.message");
            }
            project.erase(getUserWorkspace().getUser(), comment);
            designRepositoryAclService.deleteAcl(project);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    private void openAllDependencies(RulesProject project) throws ProjectException {
        for (RulesProject rulesProject : projectDependencyResolver.getProjectDependencies(project)) {
            rulesProject.open();
        }
    }

    private static class ProjectInfo {

        ProjectInfo(RulesProject project) {
            this.name = project.getBusinessName();
            this.modified = project.isModified();
            this.opened = project.isOpened();
            this.localOnly = project.isLocalOnly();
            this.deleted = project.isDeleted();
            this.openedForEditing = project.isOpenedForEditing();
        }

        public String name;
        public boolean modified;
        public boolean opened;
        public boolean localOnly;
        public boolean openedForEditing;
        public boolean deleted;
        public List<ProjectInfo> dependencies;
        public List<ProjectInfo> dependsOn;
    }

}
