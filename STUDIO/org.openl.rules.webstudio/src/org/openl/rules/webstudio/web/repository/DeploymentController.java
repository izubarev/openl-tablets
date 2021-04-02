package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;

/**
 * Deployment controller.
 *
 * @author Andrey Naumenko
 */
@Service
@ViewScope
public class DeploymentController {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentController.class);
    private List<DeploymentDescriptorItem> items;
    private String repositoryId;
    private String projectName;
    private String projectBranch;
    private String version;
    private String cachedForProject;
    private String repositoryConfigName;
    private boolean canDeploy;

    @Autowired
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;

    @Autowired
    private RepositoryTreeState repositoryTreeState;

    @Autowired
    private DeploymentManager deploymentManager;

    @Autowired
    private volatile ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    private PropertyResolver propertyResolver;

    @Autowired
    @Qualifier("deployConfigRepositoryComments")
    private Comments deployConfigRepoComments;

    public void onPageLoad() {
        if (repositoryTreeState == null || getSelectedProject() == null) {
            canDeploy = false;
            return;
        }

        if (!repositoryTreeState.getCanDeploy()) {
            canDeploy = false;
        } else {
            DependencyChecker checker = new DependencyChecker(projectDescriptorResolver);
            ADeploymentProject project = getSelectedProject();
            synchronized (project) {
                checker.addProjects(project);
            }
            canDeploy = checker.check();
        }
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public String addItem(String version) {
        this.version = version;
        ADeploymentProject project = getSelectedProject();
        if (project == null) {
            return null;
        }

        try {
            UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
            AProject projectToAdd = workspace.getDesignTimeRepository().getProject(repositoryId, projectName);
            String businessName = projectToAdd.getBusinessName();
            String path = projectToAdd.getRealPath();

                ProjectDescriptorImpl newItem = new ProjectDescriptorImpl(
                        repositoryId,
                        businessName,
                        path,
                        projectBranch,
                        new CommonVersionImpl(version)
                );
                List<ProjectDescriptor> newDescriptors = replaceDescriptor(project, businessName, newItem);
                synchronized (project) {
                    items = null;
                    project.setProjectDescriptors(newDescriptors);
                }
            } catch (ProjectException e) {
                LOG.error("Failed to add project descriptor.", e);
                WebStudioUtils.addErrorMessage("failed to add project descriptor", e.getMessage());
            }

        return null;
    }

    private void checkConflicts(List<DeploymentDescriptorItem> items) {
        if (items == null) {
            return;
        }

        DependencyChecker checker = new DependencyChecker(projectDescriptorResolver);
        ADeploymentProject project = getSelectedProject();
        synchronized (project) {
            checker.addProjects(project);
        }
        checker.check(items);
    }

    public synchronized boolean isCanDeploy() {
        return canDeploy;
    }

    public String save() {
        try {
            ADeploymentProject selectedProject = getSelectedProject();
            if (selectedProject == null) {
                WebStudioUtils.addErrorMessage("Deployment configuration isn't selected");
                return null;
            }
            synchronized (selectedProject) {
                String comment = deployConfigRepoComments.saveProject(selectedProject.getName());
                selectedProject.getFileData().setComment(comment);
                selectedProject.save();
            }
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to save changes", e);
            WebStudioUtils.addErrorMessage("Failed to save changes", e.getMessage());
        }

        return null;
    }

    public String open() {
        try {
            getSelectedProject().open();
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to open", e);
            WebStudioUtils.addErrorMessage("Failed to open", e.getMessage());
        }

        return null;
    }

    public String close() {
        try {
            getSelectedProject().close();
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to close.", e);
            WebStudioUtils.addErrorMessage("failed to close deployment project", e.getMessage());
        }

        return null;
    }

    public String deleteItem() {
        String projectName = WebStudioUtils.getRequestParameter("key");
        ADeploymentProject project = getSelectedProject();

        try {
            List<ProjectDescriptor> newDescriptors = replaceDescriptor(project, projectName, null);
            synchronized (project) {
                items = null;
                project.setProjectDescriptors(newDescriptors);
            }
        } catch (ProjectException e) {
            LOG.error("Failed to delete project descriptor.", e);
            WebStudioUtils.addErrorMessage("failed to add project descriptor", e.getMessage());
        }
        return null;
    }

    public String deploy() {
        ADeploymentProject project = getSelectedProject();
        if (project != null) {
            if (project.getProjectDescriptors().isEmpty()){
                WebStudioUtils.addErrorMessage(
                    String.format(
                            "Configuration '%s' should contain at least one project to be deployed",
                            project.getName()
                    )
                );
                return null;
            }
            RepositoryConfiguration repo = new RepositoryConfiguration(repositoryConfigName, propertyResolver);

            try {
                DeployID id = deploymentManager.deploy(project, repositoryConfigName);
                String message = String.format(
                    "Configuration '%s' is successfully deployed with id '%s' to repository '%s'",
                    project.getName(),
                    id.getName(),
                    repo.getName());
                WebStudioUtils.addInfoMessage(message);

                productionRepositoriesTreeController.refreshTree();
            } catch (Exception e) {
                String msg = String
                    .format("Failed to deploy '%s' to repository '%s'", project.getName(), repo.getName());
                LOG.error(msg, e);
                WebStudioUtils.addErrorMessage(msg, e.getMessage());
            }
        }
        return null;
    }

    public List<DeploymentDescriptorItem> getItems() {
        ADeploymentProject project = getSelectedProject();
        if (project == null) {
            return null;
        }

        String projectNameWithVersion = project.getName() + project.getVersion().getVersionName();
        if (items != null && projectNameWithVersion.equals(cachedForProject)) {
            return items;
        }

        cachedForProject = projectNameWithVersion;
        synchronized (project) {
            Collection<ProjectDescriptor> descriptors = project.getProjectDescriptors();
            items = new ArrayList<>();

            for (ProjectDescriptor descriptor : descriptors) {
                DeploymentDescriptorItem item = new DeploymentDescriptorItem(
                        descriptor.getRepositoryId(),
                        descriptor.getProjectName(),
                        descriptor.getPath(),
                        descriptor.getProjectVersion()
                );
                items.add(item);
            }
        }

        checkConflicts(items);

        return items;
    }

    public String getProjectName() {
        return projectName;
    }

    public SelectItem[] getProjects() {
        if (repositoryId == null) {
            return new SelectItem[0];
        }
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        // Because of JSF this method can be invoked up to 24 times during 1 HTTP request.
        // That's why we must not refresh projects list every time, instead get cached projects only.
        Collection<RulesProject> workspaceProjects = workspace.getProjects(false)
            .stream()
            .filter(p -> repositoryId.equals(p.getRepository().getId()))
            .collect(Collectors.toCollection(ArrayList::new));
        List<SelectItem> selectItems = new ArrayList<>();

        List<DeploymentDescriptorItem> existingItems = getItems();
        Set<String> existing = new HashSet<>();
        if (existingItems != null) {
            for (DeploymentDescriptorItem ddItem : existingItems) {
                existing.add(ddItem.getName());
            }
        }

        for (RulesProject project : workspaceProjects) {
            if (!(existing.contains(project.getBusinessName()) || project.isLocalOnly() || project.isDeleted())) {
                selectItems.add(new SelectItem(project.getName(), project.getBusinessName()));
            }
        }

        return selectItems.toArray(new SelectItem[0]);
    }

    /*
     * Deprecated public SelectItem[] getProjectVersions() { UserWorkspace workspace = RepositoryUtils.getWorkspace();
     * if (projectName != null) { try { AProject project = workspace.getProject(projectName); // sort project versions
     * in descending order (1.1 -> 0.0) List<ProjectVersion> versions = new
     * ArrayList<ProjectVersion>(project.getVersions()); Collections.sort(versions,
     * RepositoryUtils.VERSIONS_REVERSE_COMPARATOR);
     *
     * List<SelectItem> selectItems = new ArrayList<SelectItem>(); for (ProjectVersion version. : versions) {
     * selectItems.add(new SelectItem(version.getVersionName())); } return selectItems.toArray(new
     * SelectItem[selectItems.size()]); } catch (ProjectException e) { log.error("Failed to get project versions.", e);
     * } } return new SelectItem[0]; }
     */
    public List<ProjectVersion> getProjectVersions() {
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());

        if (projectName != null) {
            try {
                AProject project = workspace.getDesignTimeRepository().getProject(repositoryId, projectName);
                Repository repository = project.getRepository();
                if (repository.supports().branches()) {
                    BranchRepository branchRepository = (BranchRepository) repository;
                    if (projectBranch != null && !projectBranch.equals(branchRepository.getBranch())) {
                        project = new AProject(branchRepository.forBranch(projectBranch), project.getFolderPath());
                    }
                }
                // sort project versions in descending order (1.1 -> 0.0)
                List<ProjectVersion> versions = new ArrayList<>(project.getVersions());
                Collections.reverse(versions);

                return versions;
            } catch (Exception e) {
                LOG.error("Failed to get project versions.", e);
            }
        }

        return Collections.emptyList();
    }

    private ADeploymentProject getSelectedProject() {
        AProjectArtefact artefact = repositoryTreeState.getSelectedNode().getData();
        if (artefact instanceof ADeploymentProject) {
            return (ADeploymentProject) artefact;
        }
        return null;
    }

    public String getVersion() {
        return version;
    }

    public String openSelectedProjects() {
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        for (DeploymentDescriptorItem item : items) {
            if (item.isSelected()) {
                String projectName = item.getName();
                try {
                    RulesProject project = workspace.getProject(item.getRepositoryId(), projectName, false);
                    if (!project.isModified()) {
                        project.openVersion(item.getVersion().getVersionName());
                    }
                    repositoryTreeState.refreshNode(
                        repositoryTreeState.getRulesRepository().getChild(RepositoryUtils.getTreeNodeId(project)));
                } catch (Exception e) {
                    LOG.error("Failed to open project '{}'.", projectName, e);
                    WebStudioUtils.addErrorMessage("Failed to open project '" + projectName + "': " + e.getMessage());
                }
            }
            item.setSelected(false);
            WebStudioUtils.getWebStudio().reset();
        }
        return null;
    }

    private List<ProjectDescriptor> replaceDescriptor(ADeploymentProject project,
            String projectName,
            ProjectDescriptorImpl newItem) {
        List<ProjectDescriptor> newDescriptors = new ArrayList<>();

        for (ProjectDescriptor pd : project.getProjectDescriptors()) {
            if (pd.getProjectName().equals(projectName)) {
                if (newItem != null) {
                    newDescriptors.add(newItem);
                    newItem = null;
                }
            } else {
                newDescriptors.add(pd);
            }
        }
        if (newItem != null) {
            newDescriptors.add(newItem);
        }
        return newDescriptors;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;

        initBranch();
    }

    private void initBranch() {
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        Repository repository = workspace.getDesignTimeRepository().getRepository(repositoryId);
        if (repository.supports().branches()) {
            projectBranch = ((BranchRepository) repository).getBranch();
        }
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isModified() {
        return getSelectedProject().isModified();
    }

    public String getRepositoryConfigName() {
        return repositoryConfigName;
    }

    public void setRepositoryConfigName(String repositoryConfigName) {
        this.repositoryConfigName = repositoryConfigName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        SelectItem[] projects = getProjects();
        if (projects.length > 0) {
            this.projectName = (String) projects[0].getValue();
        } else {
            this.projectName = null;
        }
        initBranch();
    }

    public Collection<RepositoryConfiguration> getRepositories() {
        List<RepositoryConfiguration> repos = new ArrayList<>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, propertyResolver);
            repos.add(config);
        }

        repos.sort(RepositoryConfiguration.COMPARATOR);
        return repos;
    }

    public void initAddDeployItemDialog() {
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        List<Repository> repositories = workspace.getDesignTimeRepository().getRepositories();
        setRepositoryId(repositories.isEmpty() ? null : repositories.get(0).getId());
    }

    public ProductionRepositoriesTreeController getProductionRepositoriesTreeController() {
        return productionRepositoriesTreeController;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }

    public void setDeployConfigRepoComments(Comments deployConfigRepoComments) {
        this.deployConfigRepoComments = deployConfigRepoComments;
    }

    public boolean isRepoSupportsBranches() {
        if (repositoryId == null) {
            return false;
        }
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        Repository repository = workspace.getDesignTimeRepository().getRepository(repositoryId);
        return repository != null && repository.supports().branches();
    }

    public String getProjectBranch() {
        return projectBranch;
    }

    public void setProjectBranch(String projectBranch) {
        this.projectBranch = projectBranch;
    }

    public List<String> getProjectBranches() {
        try {
            UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());

            Repository repository = workspace.getDesignTimeRepository().getRepository(repositoryId);
            if (!repository.supports().branches()) {
                return Collections.emptyList();
            }

            String rulesPath = workspace.getDesignTimeRepository().getRulesLocation();
            List<String> branches = new ArrayList<>(((BranchRepository) repository).getBranches(rulesPath + projectName));
            if (projectBranch != null && !branches.contains(projectBranch)) {
                branches.add(projectBranch);
                branches.sort(String.CASE_INSENSITIVE_ORDER);
            }

            return branches;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
