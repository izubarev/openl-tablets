package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.filter.AllFilter;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.filter.RepositoryFileExtensionFilter;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeFile;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.util.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.richfaces.model.SequenceRowKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
@Service
@SessionScope
public class RepositoryTreeState implements DesignTimeRepositoryListener {
    private static final String ROOT_TYPE = "root";

    @Autowired
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;
    @Autowired
    private ProjectDescriptorArtefactResolver projectDescriptorResolver;

    private static final String DEFAULT_TAB = "Properties";
    private final Logger log = LoggerFactory.getLogger(RepositoryTreeState.class);
    private static final IFilter<AProjectArtefact> ALL_FILTER = new AllFilter<>();

    private RepositorySelectNodeStateHolder.SelectionHolder selectionHolder;
    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;

    private UserWorkspace userWorkspace;

    private IFilter<AProjectArtefact> filter = ALL_FILTER;
    private boolean hideDeleted = true;
    private String filterString;
    private String filterRepositoryId;

    private final Object lock = new Object();
    private String errorMessage;
    private final WorkspaceListener workspaceListener = new WorkspaceListener();

    private void buildTree() {
        try {
            if (root != null) {
                return;
            }
            log.debug("Starting buildTree()");

            root = new TreeRepository("", "", filter, ROOT_TYPE);

            String projectsTreeId = "1st - Projects";
            String rpName = "Projects";
            rulesRepository = new TreeRepository(projectsTreeId, rpName, filter, UiConst.TYPE_REPOSITORY);
            rulesRepository.setData(null);

            String deploymentsTreeId = "2nd - Deploy Configurations";
            String dpName = "Deploy Configurations";
            deploymentRepository = new TreeRepository(deploymentsTreeId,
                dpName,
                filter,
                UiConst.TYPE_DEPLOYMENT_REPOSITORY);
            deploymentRepository.setData(null);

            // Such keys are used for correct order of repositories.
            root.add(rulesRepository);
            root.add(deploymentRepository);

            Collection<RulesProject> rulesProjects = userWorkspace.getProjects();

            IFilter<AProjectArtefact> filter = this.filter;
            for (AProject project : rulesProjects) {
                if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                    addRulesProjectToTree(project);
                }
            }
            if (rulesProjects.isEmpty()) {
                // Initialize content of empty node
                rulesRepository.getElements();
            }

            try {
                List<ADeploymentProject> deployConfigurations = userWorkspace.getDDProjects();
                for (ADeploymentProject project : deployConfigurations) {
                    addDeploymentProjectToTree(project);
                }
                if (deployConfigurations.isEmpty()) {
                    // Initialize content of empty node
                    deploymentRepository.getElements();
                }
            } catch (ProjectException e) {
                log.error("Cannot get deployment projects", e);
            }
            log.debug("Finishing buildTree()");

            if (getSelectedNode() == null || UiConst.TYPE_REPOSITORY.equals(getSelectedNode().getType())) {
                setSelectedNode(rulesRepository);
            } else if (UiConst.TYPE_DEPLOYMENT_REPOSITORY.equals(getSelectedNode().getType())) {
                setSelectedNode(deploymentRepository);
            } else {
                updateSelectedNode();
            }
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message = "Cannot build repository tree. " + (rootCause == null ? e.getMessage()
                                                                                   : rootCause.getMessage());
            log.error(message, e);
            errorMessage = message;
            setSelectedNode(rulesRepository);
        }
    }

    public TreeRepository getDeploymentRepository() {
        synchronized (lock) {
            buildTree();
            return deploymentRepository;
        }
    }

    TreeRepository getRoot() {
        synchronized (lock) {
            buildTree();
            return root;
        }
    }

    String getErrorMessage() {
        return errorMessage;
    }

    public TreeRepository getRulesRepository() {
        synchronized (lock) {
            buildTree();
            return rulesRepository;
        }
    }

    public TreeNode getSelectedNode() {
        synchronized (lock) {
            buildTree();
            return this.repositorySelectNodeStateHolder.getSelectedNode();
        }
    }

    public Collection<SequenceRowKey> getSelection() {
        TreeNode node = getSelectedNode();

        List<String> ids = new ArrayList<>();
        while (node != null && !node.getType().equals(ROOT_TYPE)) {
            ids.add(0, node.getId());
            node = node.getParent();
        }

        return new ArrayList<>(Collections.singletonList(new SequenceRowKey(ids.toArray())));
    }

    TreeProject getProjectNodeByBusinessName(String repoId, String businessName) {
        for (TreeNode treeNode : getRulesRepository().getChildNodes()) {
            TreeProject project = (TreeProject) treeNode;
            if (((RulesProject) project.getData()).getBusinessName().equals(businessName)) {
                if (repoId == null || repoId.equals(project.getData().getRepository().getId())) {
                    return project;
                }
            }
        }
        return null;
    }

    public TreeProject getProjectNodeByPhysicalName(String repoId, String physicalName) {
        for (TreeNode treeNode : getRulesRepository().getChildNodes()) {
            TreeProject project = (TreeProject) treeNode;
            if (project.getData().getName().equals(physicalName)) {
                if (repoId == null || repoId.equals(project.getData().getRepository().getId())) {
                    return project;
                }
            }
        }
        return null;
    }

    public UserWorkspaceProject getSelectedProject() {
        return getProject(getSelectedNode());
    }

    public UserWorkspaceProject getProject(TreeNode node) {
        AProjectArtefact artefact = node.getData();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        } else if (artefact != null && artefact.getProject() instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact.getProject();
        }

        return null;
    }

    public boolean isSelectedProjectModified() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject != null && selectedProject.isModified();
    }

    public void invalidateSelection() {
        setSelectedNode(rulesRepository);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode after rebuilding tree.
     */
    private void updateSelectedNode() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact == null) {
            return;
        }

        String branch = null;
        AProject project = artefact.getProject();
        if (project instanceof UserWorkspaceProject) {
            branch = ((UserWorkspaceProject) project).getBranch();
        }

        String repoId = artefact.getRepository().getId();
        Iterator<String> it = artefact.getArtefactPath().getSegments().iterator();
        TreeNode currentNode = getRulesRepository();
        while (currentNode != null && it.hasNext()) {
            String id = RepositoryUtils.getTreeNodeId(repoId, it.next());
            currentNode = (TreeNode) currentNode.getChild(id);

            if (branch != null && currentNode != null) {
                // If currentNode is a project, update its branch.
                AProjectArtefact currentArtefact = currentNode.getData();
                if (currentArtefact instanceof UserWorkspaceProject) {
                    UserWorkspaceProject newProject = (UserWorkspaceProject) currentArtefact;
                    if (!branch.equals(newProject.getBranch())) {
                        try {
                            RulesProject rulesProject = (RulesProject) project;
                            boolean containsBranch = ((BranchRepository) rulesProject.getDesignRepository())
                                .getBranches(((RulesProject) project).getDesignFolderName())
                                .contains(branch);
                            if (containsBranch) {
                                // Update branch for the project
                                newProject.setBranch(branch);
                                // Rebuild children for the node
                                currentNode.refresh();
                            }
                        } catch (ProjectException | IOException e) {
                            log.error("Cannot update selected node: {}", e.getMessage(), e);
                        }
                    }
                }
            }
        }

        if (currentNode != null) {
            setSelectedNode(currentNode);
        } else {
            invalidateSelection();
        }
    }

    public void refreshNode(TreeNode node) {
        node.refresh();
    }

    public void deleteNode(TreeNode node) {
        node.getParent().removeChild(node.getId());
    }

    public void deleteSelectedNodeFromTree() {
        synchronized (lock) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != root && selectedNode != rulesRepository && selectedNode != deploymentRepository) {
                deleteNode(selectedNode);
                moveSelectionToParentNode();
            }
        }
    }

    public void addDeploymentProjectToTree(ADeploymentProject project) {
        String name = project.getName();
        String id = RepositoryUtils.getTreeNodeId(project);
        if (!project.isDeleted() || !hideDeleted) {
            TreeDProject prj = new TreeDProject(id, name);
            prj.setData(project);
            deploymentRepository.add(prj);
        }
    }

    public void addRulesProjectToTree(AProject project) {
        String name = project.getBusinessName();
        String id = RepositoryUtils.getTreeNodeId(project);
        if (!project.isDeleted() || !hideDeleted) {
            TreeProject prj = new TreeProject(id, name, filter, projectDescriptorResolver);
            prj.setData(project);
            rulesRepository.add(prj);
        }
    }

    public void addNodeToTree(TreeNode parent, AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = RepositoryUtils.getTreeNodeId(childArtefact);
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, name, filter);
            treeFolder.setData(childArtefact);
            parent.add(treeFolder);
        } else {
            TreeFile treeFile = new TreeFile(id, name);
            treeFile.setData(childArtefact);
            parent.add(treeFile);
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        synchronized (lock) {
            root = null;
            errorMessage = null;

            // Clear all ViewScoped beans that could cache some temporary values (for example DeploymentController).
            // Because selection is invalidated too we can assume that view is changed so we can safely clear all
            // views scoped beans.
            FacesContext.getCurrentInstance().getViewRoot().getViewMap().clear();
        }
    }

    /**
     * Moves selection to the parent of the current selected node.
     */
    private void moveSelectionToParentNode() {
        if (getSelectedNode().getParent() != null) {
            setSelectedNode(getSelectedNode().getParent());
        } else {
            invalidateSelection();
        }
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<>(event.getNewSelection());

        /* If there are no selected nodes */
        if (selection.isEmpty()) {
            return;
        }

        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        setSelectedNode((TreeNode) tree.getRowData());
        tree.setRowKey(storedKey);
    }

    public void rulesRepositorySelection() {
        setSelectedNode(rulesRepository);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        refreshNode(getSelectedNode());
    }

    public void setFilter(IFilter<AProjectArtefact> filter) {
        this.filter = filter != null ? filter : ALL_FILTER;
        synchronized (lock) {
            root = null;
            errorMessage = null;
        }
    }

    public void filter() {
        IFilter<AProjectArtefact> filter = null;
        if (StringUtils.isNotBlank(filterString)) {
            filter = new RepositoryFileExtensionFilter(filterString);
        }
        IFilter<AProjectArtefact> repositoryFilter = null;
        if (StringUtils.isNotBlank(filterRepositoryId)) {
            repositoryFilter = new RepositoryFilter(filterRepositoryId);
        }
        if (repositoryFilter != null) {
            if (filter != null) {
                filter = new AndFilterIfSupport(repositoryFilter, filter);
            } else {
                filter = repositoryFilter;
            }
        }
        setFilter(filter);
        setHideDeleted(hideDeleted);
    }

    public void setFilter(String filterString, String filterRepositoryId, boolean hideDeleted) {
        this.filter = filter != null ? filter : ALL_FILTER;
        synchronized (lock) {
            root = null;
            errorMessage = null;
        }
    }

    public void setSelectedNode(TreeNode selectedNode) {
        selectionHolder.setSelectedNode(selectedNode);
    }

    @PostConstruct
    public void init() {
        selectionHolder = repositorySelectNodeStateHolder.getSelectionHolder();

        this.userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        userWorkspace.getDesignTimeRepository().addListener(this);
        userWorkspace.addWorkspaceListener(workspaceListener);
    }

    @PreDestroy
    public void destroy() {
        if (userWorkspace != null) {
            userWorkspace.getDesignTimeRepository().removeListener(this);
            userWorkspace.removeWorkspaceListener(workspaceListener);
        }
    }

    @Override
    public void onRepositoryModified() {
        Collection<RulesProject> projects;
        List<ADeploymentProject> deployConfigs;
        try {
            projects = userWorkspace.getProjects(false);
            deployConfigs = userWorkspace.getDDProjects();
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
            return;
        }
        synchronized (lock) {
            // We must not refresh the table when getting selected node.
            TreeNode selectedNode = selectionHolder.getSelectedNode();
            AProjectArtefact artefact = selectedNode == null ? null : selectedNode.getData();
            if (artefact != null) {
                AProject project = artefact instanceof UserWorkspaceProject ? (UserWorkspaceProject) artefact
                                                                            : artefact.getProject();

                String name = project.getName();
                if (project instanceof RulesProject) {
                    // We cannot use hasProject() and then getProject(name) in multithreaded environment
                    invalidateSelectionIfDeleted(name, projects);
                } else if (project instanceof ADeploymentProject) {
                    // We cannot use hasDDProject() and then getDDProject(name) in multithreaded environment
                    invalidateSelectionIfDeleted(name, deployConfigs);
                }
            }

            root = null;
            errorMessage = null;
        }
    }

    private void invalidateSelectionIfDeleted(String name,
            Collection<? extends UserWorkspaceProject> existingProjects) {
        UserWorkspaceProject existing = null;
        for (UserWorkspaceProject existingProject : existingProjects) {
            if (name.equals(existingProject.getName())) {
                existing = existingProject;
            }
        }
        if (existing == null || existing.isDeleted() && isHideDeleted()) {
            invalidateSelection();
        }
    }

    public boolean isHideDeleted() {
        return hideDeleted;
    }

    public void setHideDeleted(boolean hideDeleted) {
        this.hideDeleted = hideDeleted;
    }

    public String getFilterString() {
        return filterString;
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public String getFilterRepositoryId() {
        return filterRepositoryId;
    }

    public void setFilterRepositoryId(String filterRepositoryId) {
        this.filterRepositoryId = filterRepositoryId;
    }

    public boolean getCanCreate() {
        return isGranted(CREATE_PROJECTS);
    }

    // For any project
    public boolean getCanEdit() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject
            .isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(EDIT_PROJECTS);
    }

    public boolean getCanCreateDeployment() {
        return isGranted(CREATE_DEPLOYMENT);
    }

    public boolean getCanEditDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(EDIT_DEPLOYMENT);
    }

    public boolean getCanDeleteDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        return (!selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser())) && isGranted(
            DELETE_DEPLOYMENT);
    }

    public boolean getCanSaveDeployment() {
        ADeploymentProject selectedProject = (ADeploymentProject) getSelectedProject();
        return selectedProject.isOpenedForEditing() && selectedProject.isModified() && isGranted(EDIT_DEPLOYMENT);
    }

    public boolean getCanSaveProject() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            return selectedProject != null && selectedProject.isModified() && isGranted(EDIT_PROJECTS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanClose() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject != null) {
            return !selectedProject.isLocalOnly() && selectedProject.isOpened();
        } else {
            return false;
        }
    }

    public boolean getCanDelete() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject.isLocalOnly()) {
                // any user can delete own local project
                return true;
            }
            boolean unlocked = !selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser());
            boolean mainBranch = isMainBranch(selectedProject);
            return unlocked && isGranted(DELETE_PROJECTS) && mainBranch;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanDeleteBranch() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject.isLocalOnly()) {
                return false;
            }
            boolean unlocked = !selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser());
            boolean mainBranch = isMainBranch(selectedProject);
            return unlocked && isGranted(DELETE_PROJECTS) && !mainBranch;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean isMainBranch(UserWorkspaceProject selectedProject) {
        boolean mainBranch = true;
        Repository designRepository = selectedProject.getDesignRepository();
        if (designRepository.supports().branches()) {
            String branch = selectedProject.getBranch();
            if (!((BranchRepository) designRepository).getBaseBranch().equals(branch)) {
                mainBranch = false;
            }
        }
        return mainBranch;
    }

    public boolean getCanErase() {
        return getSelectedProject().isDeleted() && isGranted(ERASE_PROJECTS);
    }

    public boolean getCanOpen() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject
                .isOpenedForEditing() || selectedProject.isOpened()) {
                return false;
            }

            return isGranted(VIEW_PROJECTS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanOpenOtherVersion() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject == null) {
            return false;
        }

        if (!selectedProject.isLocalOnly()) {
            return isGranted(VIEW_PROJECTS);
        }

        return false;
    }

    public boolean getCanExport() {
        return !getSelectedProject().isLocalOnly();
    }

    public boolean getCanCompare() {
        if (getSelectedProject().isLocalOnly()) {
            return false;
        }
        return isGranted(VIEW_PROJECTS);
    }

    public boolean getCanRedeploy() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isModified()) {
                return false;
            }

            return isGranted(DEPLOY_PROJECTS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanUndelete() {
        return getSelectedProject().isDeleted() && isGranted(EDIT_PROJECTS);
    }

    // for any project artefact
    public boolean getCanModify() {
        AProjectArtefact selectedArtefact = getSelectedNode().getData();
        String projectId = RepositoryUtils.getTreeNodeId(selectedArtefact.getProject());
        RulesProject project = (RulesProject) getRulesRepository().getChild(projectId).getData();
        return project.isOpenedForEditing() && isGranted(EDIT_PROJECTS);
    }

    // for deployment project
    public boolean getCanDeploy() {
        return !getSelectedProject().isModified() && isGranted(DEPLOY_PROJECTS);
    }

    public boolean getCanMerge() {
        if (!isSupportsBranches() || isLocalOnly()) {
            return false;
        }

        try {
            UserWorkspaceProject project = getSelectedProject();
            if (project.isModified() || !(project instanceof RulesProject)) {
                return false;
            }
            List<String> branches = ((BranchRepository) project.getDesignRepository())
                .getBranches(((RulesProject) project).getDesignFolderName());
            if (branches.size() < 2) {
                return false;
            }

            return isGranted(EDIT_PROJECTS);
        } catch (IOException e) {
            return false;
        }
    }

    public String getDefSelectTab() {
        return DEFAULT_TAB;
    }

    public boolean isLocalOnly() {
        return getSelectedProject().isLocalOnly();
    }

    public String clearSelectPrj() {
        synchronized (lock) {
            buildTree();
            invalidateSelection();
        }

        return "";
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    /**
     * Returns true if both are true: 1) Old project version is opened and 2) project is not modified yet.
     *
     * Otherwise return false
     */
    public boolean isConfirmOverwriteNewerRevision() {
        UserWorkspaceProject project = getSelectedProject();
        return project != null && project.isOpenedOtherVersion() && !project.isModified();
    }

    /**
     * Checks if selected project supports branches
     */
    public boolean isSupportsBranches() {
        try {
            UserWorkspaceProject project = getSelectedProject();
            return project != null && project.isSupportsBranches();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private class WorkspaceListener implements UserWorkspaceListener {
        @Override
        public void workspaceReleased(UserWorkspace workspace) {
            synchronized (lock) {
                root = null;
            }
        }

        @Override
        public void workspaceRefreshed() {
            synchronized (lock) {
                root = null;
            }
        }
    }
}
