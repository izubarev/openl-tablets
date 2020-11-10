package org.openl.rules.workspace.uw.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserWorkspaceImpl implements UserWorkspace {
    private final Logger log = LoggerFactory.getLogger(UserWorkspaceImpl.class);

    private static final Comparator<AProject> PROJECTS_COMPARATOR = (o1, o2) -> {
        int compare = o1.getBusinessName().compareToIgnoreCase(o2.getBusinessName());
        if (compare != 0) {
            return compare;
        }
        compare = o1.getRepository().getId().compareTo(o2.getRepository().getId());
        if (compare != 0) {
            return compare;
        }
        compare = o1.getRealPath().compareToIgnoreCase(o2.getRealPath());
        return compare;
    };

    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;

    private final HashMap<ProjectKey, RulesProject> userRulesProjects;
    private final HashMap<String, ADeploymentProject> userDProjects;

    private boolean projectsRefreshNeeded = true;
    private boolean deploymentsRefreshNeeded = true;
    private boolean cleanUpOnActivation = false;

    private final List<UserWorkspaceListener> listeners = new ArrayList<>();
    private final LockEngine projectsLockEngine;
    private final LockEngine deploymentsLockEngine;
    private final DesignTimeRepositoryListener designRepoListener = this::refresh;

    public UserWorkspaceImpl(WorkspaceUser user,
                             LocalWorkspace localWorkspace,
                             DesignTimeRepository designTimeRepository,
                             LockEngine projectsLockEngine,
                             LockEngine deploymentsLockEngine) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.projectsLockEngine = projectsLockEngine;
        this.deploymentsLockEngine = deploymentsLockEngine;

        userRulesProjects = new HashMap<>();
        userDProjects = new HashMap<>();
    }

    @Override
    public void activate() {
        cleanUpOnActivation = true;
        refresh();
    }

    @Override
    public void addWorkspaceListener(UserWorkspaceListener listener) {
        listeners.add(listener);
        designTimeRepository.addListener(designRepoListener);
    }

    @Override
    public void copyDDProject(ADeploymentProject project, String name, String comment) throws ProjectException {
        ADeploymentProject newProject = designTimeRepository.createDeploymentConfigurationBuilder(name)
                .user(getUser())
                .lockEngine(deploymentsLockEngine)
                .build();
        newProject.getFileData().setComment(comment);
        newProject.update(project, user);

        refresh();
    }

    @Override
    public ADeploymentProject createDDProject(String name) throws RepositoryException {
        if (deploymentsRefreshNeeded) {
            refreshDeploymentProjects();
        }
        ADeploymentProject ddProject = designTimeRepository.createDeploymentConfigurationBuilder(name)
                .user(getUser())
                .lockEngine(deploymentsLockEngine)
                .build();
        userDProjects.put(name.toLowerCase(), ddProject);
        return ddProject;
    }

    @Override
    public ADeploymentProject getDDProject(String name) throws ProjectException {
        refreshDeploymentProjects();
        ADeploymentProject deploymentProject;
        synchronized (userDProjects) {
            deploymentProject = userDProjects.get(name.toLowerCase());
        }
        if (deploymentProject == null) {
            throw new ProjectException("Cannot find deployment project ''{0}''", null, name);
        }
        return deploymentProject;
    }

    @Override
    public ADeploymentProject getLatestDeploymentConfiguration(String name) {
        return getDesignTimeRepository().createDeploymentConfigurationBuilder(name)
                .user(getUser())
                .lockEngine(deploymentsLockEngine)
                .build();
    }

    @Override
    public List<ADeploymentProject> getDDProjects() throws ProjectException {
        refreshDeploymentProjects();

        ArrayList<ADeploymentProject> result;
        synchronized (userDProjects) {
            result = new ArrayList<>(userDProjects.values());
        }
        result.sort(PROJECTS_COMPARATOR);

        return result;
    }

    @Override
    public DesignTimeRepository getDesignTimeRepository() {
        return designTimeRepository;
    }

    // --- protected

    @Override
    public LocalWorkspace getLocalWorkspace() {
        return localWorkspace;
    }

    @Override
    public RulesProject getProject(String repositoryId, String name) throws ProjectException {
        return getProject(repositoryId, name, true);
    }

    @Override
    public RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }

        RulesProject uwp;
        synchronized (userRulesProjects) {
            uwp = userRulesProjects.get(new ProjectKey(repositoryId, name.toLowerCase()));
        }

        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}''", null, name);
        }

        return uwp;
    }

    @Override
    public Collection<RulesProject> getProjects() {
        return getProjects(true);
    }

    @Override
    public Collection<RulesProject> getProjects(boolean refreshBefore) {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }

        ArrayList<RulesProject> result;
        synchronized (userRulesProjects) {
            result = new ArrayList<>(userRulesProjects.values());
        }

        result.sort(PROJECTS_COMPARATOR);

        return result;
    }

    @Override
    public WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean hasDDProject(String name) {
        if (deploymentsRefreshNeeded) {
            try {
                refreshDeploymentProjects();
            } catch (RepositoryException e) {
                // FIXME Don't wrap checked exception
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
        synchronized (userDProjects) {
            if (userDProjects.get(name.toLowerCase()) != null) {
                return true;
            }
        }
        return designTimeRepository.hasDDProject(name);
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (userRulesProjects) {
            if (projectsRefreshNeeded) {
                refreshRulesProjects();
            }
            return userRulesProjects.containsKey(new ProjectKey(repositoryId, name.toLowerCase()));
        }
    }

    @Override
    public void passivate() {
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();

        cleanUpOnActivation = false;
    }

    @Override
    public void refresh() {
        localWorkspace.refresh();
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();
    }

    @Override
    public void syncProjects() {
        for (RulesProject rPr : getProjects()) {
            String designFolderName = rPr.getDesignFolderName();
            if (designFolderName != null && designTimeRepository.getRepository(rPr.getRepository().getId()).supports().mappedFolders()) {
                String masterProjectName = designFolderName.substring(designFolderName.lastIndexOf('/') + 1, designFolderName.lastIndexOf(':'));
                if (rPr.isOpened() && !rPr.getLocalFolderName().equals(masterProjectName)) {
                    try {
                        rPr.close();
                        rPr.setLocalFolderName(null);
                        rPr.open();
                    } catch (ProjectException e) {
                        log.warn("Could not reopen the project '{}' because of error: {}",
                                rPr.getName(),
                                e.getMessage());
                    }
                }
            }
        }
    }

    private void scheduleDeploymentsRefresh() {
        synchronized (userDProjects) {
            deploymentsRefreshNeeded = true;
        }
    }

    private void refreshDeploymentProjects() throws RepositoryException {
        List<ADeploymentProject> dtrProjects = designTimeRepository.getDDProjects();

        synchronized (userDProjects) {
            // add new
            HashMap<String, ADeploymentProject> dtrProjectsMap = new HashMap<>();
            for (ADeploymentProject ddp : dtrProjects) {
                String name = ddp.getName();
                dtrProjectsMap.put(name, ddp);

                ADeploymentProject userDProject = userDProjects.get(name.toLowerCase());

                if (userDProject == null || ddp.isDeleted() != userDProject.isDeleted()) {
                    userDProject = new ADeploymentProject(user,
                            ddp.getRepository(),
                            ddp.getFileData(),
                            deploymentsLockEngine);
                    if (!userDProject.isOpened()) {
                        // Closed project cannot be locked.
                        // DeployConfiguration changes aren't persisted. If it closed, it means changes are lost. We can
                        // safely unlock it
                        if (userDProject.isLockedByMe()) {
                            userDProject.unlock();
                        }
                    }

                    userDProjects.put(name.toLowerCase(), userDProject);
                } else {
                    userDProject.refresh();
                }
            }

            // remove deleted
            Iterator<ADeploymentProject> i = userDProjects.values().iterator();
            while (i.hasNext()) {
                ADeploymentProject userDProject = i.next();
                String name = userDProject.getName();

                if (!dtrProjectsMap.containsKey(name)) {
                    i.remove();
                }
            }

            deploymentsRefreshNeeded = false;
        }
    }

    private void scheduleProjectsRefresh() {
        synchronized (userRulesProjects) {
            projectsRefreshNeeded = true;
        }
        for (UserWorkspaceListener listener : listeners) {
            listener.workspaceRefreshed();
        }
    }

    private void refreshRulesProjects() {
        localWorkspace.refresh();

        synchronized (userRulesProjects) {

            Map<ProjectKey, String> closedProjectBranches = new HashMap<>();
            for (Map.Entry<ProjectKey, RulesProject> entry : userRulesProjects.entrySet()) {
                ProjectKey projectKey = entry.getKey();
                final Repository designRepository = designTimeRepository.getRepository(projectKey.getRepositoryId());
                if (designRepository != null) {
                    boolean supportsBranches = designRepository.supports().branches();
                    if (supportsBranches) {
                        RulesProject project = entry.getValue();
                        // Deleted projects should be switched to default branch
                        if (!project.isOpened() && !project.isDeleted()) {
                            closedProjectBranches.put(projectKey, project.getBranch());
                        }
                    }
                }
            }

            userRulesProjects.clear();

            // add new
            for (AProject rp : designTimeRepository.getProjects()) {
                String repoId = rp.getRepository().getId();
                LocalRepository localRepository = localWorkspace.getRepository(repoId);
                String name = rp.getName();

                AProject lp = localWorkspace.getProjectForPath(repoId, rp.getRealPath());

                FileData local = lp == null ? null : lp.getFileData();

                Repository desRepo = rp.getRepository();
                FileData designFileData = rp.getFileData();
                boolean closeProject = false;

                try {
                    if (desRepo.supports().branches()) {
                        BranchRepository branchRepository = (BranchRepository) desRepo;
                        String repoBranch = branchRepository.getBranch();
                        String branch;
                        if (local != null) {
                            branch = local.getBranch();
                            if (branch == null) {
                                log.warn("Unknown branch in repository supporting branches for project {}.",
                                        local.getName());
                            }
                        } else {
                            branch = closedProjectBranches.get(new ProjectKey(repoId, name.toLowerCase()));
                        }

                        // If branch is null then keep default branch.
                        if (branch != null && !branch.equals(repoBranch)) {
                            if (branchRepository.branchExists(branch)) {
                                // We are inside alternative branch. Must change design repo info.
                                desRepo = branchRepository.forBranch(branch);
                                // Other branch — other version of file data
                                if (designFileData != null) {
                                    designFileData = desRepo.check(designFileData.getName());
                                }
                            } else if (local != null) {
                                log.debug("Close the project {} because the branch {} was removed", name, branch);
                                closeProject = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("Skip workspace changes for project '{}' because of error: {}",
                            rp.getName(),
                            e.getMessage());
                    desRepo = rp.getRepository();
                    designFileData = rp.getFileData();
                    local = null;
                }

                RulesProject project = new RulesProject(getUser(),
                        localRepository,
                        local,
                        desRepo,
                        designFileData,
                        projectsLockEngine);

                if (cleanUpOnActivation) {
                    // Clean ups after session activation (should be done only once).
                    if (!isVersionExistInHistory(project)) {
                        log.warn("The Project '{}' has a version {}, but absents in the history.",
                                project.getName(),
                                project.getHistoryVersion());
                        if (!project.isModified()) {
                            log.warn(
                                    "The project '{}' isn't modified and will be closed because it absents in the history.",
                                    project.getName());
                            closeProject = true;
                        }
                    }
                }

                if (closeProject) {
                    try {
                        project.close();
                    } catch (ProjectException e) {
                        log.warn("Can't close the project {}", project.getName(), e);
                    }
                }
                userRulesProjects.put(new ProjectKey(repoId, project.getName().toLowerCase()), project);
            }

            // LocalProjects that hasn't corresponding project in
            // DesignTimeRepository
            for (AProject lp : localWorkspace.getProjects()) {
                String repoId = lp.getRepository().getId();
                String name = lp.getName();

                if (!userRulesProjects.containsKey(new ProjectKey(repoId, name.toLowerCase()))) {
                    FileData local = lp.getFileData();
                    LocalRepository repository = (LocalRepository) lp.getRepository();
                    RulesProject project = new RulesProject(getUser(), repository, local, null, null, projectsLockEngine);
                    userRulesProjects.put(new ProjectKey(repoId, project.getName().toLowerCase()), project);
                }
            }

            projectsRefreshNeeded = false;
            cleanUpOnActivation = false;
        }
    }

    /**
     * Checks if a project's version is exist in history. Version can be absent in history if repository configuration
     * was switched to another path but projects in workspace point to revision in the previous repository.
     */
    private boolean isVersionExistInHistory(RulesProject project) {
        if (project.isLastVersion()) {
            return true;
        }

        ProjectVersion version = project.getVersion();
        boolean found = false;
        for (ProjectVersion v : project.getVersions()) {
            if (version.equals(v)) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public void release() {
        localWorkspace.release();
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();

        for (UserWorkspaceListener listener : new ArrayList<>(listeners)) {
            listener.workspaceReleased(this);
        }
    }

    @Override
    public void removeWorkspaceListener(UserWorkspaceListener listener) {
        designTimeRepository.removeListener(designRepoListener);
        listeners.remove(listener);
    }

    @Override
    public void uploadLocalProject(String repositoryId, String name, String projectFolder, String comment) throws ProjectException {
        try {
            String designPath = designTimeRepository.getRulesLocation() + name;
            FileData designData = new FileData();
            designData.setName(designPath);

            AProject createdProject = new AProject(designTimeRepository.getRepository(repositoryId), designData);
            AProject project = localWorkspace.getProject(null, name);
            project.refresh();
            if (designTimeRepository.getRepository(repositoryId).supports().mappedFolders()) {
                FileData fileData = createdProject.getFileData();
                fileData.addAdditionalData(new FileMappingData(designPath, projectFolder + name));
            }
            createdProject.getFileData().setComment(comment);
            createdProject.update(project, user);
            designData.setName(createdProject.getFolderPath());

            RulesProject rulesProject = new RulesProject(getUser(),
                    localWorkspace.getRepository(repositoryId),
                    project.getFileData(),
                    designTimeRepository.getRepository(repositoryId),
                    designData,
                    projectsLockEngine);
            rulesProject.open();

            refreshRulesProjects();
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(repositoryId, name)) {
                    designTimeRepository.getProject(repositoryId, name).erase(user, comment);
                }
            } catch (ProjectException e1) {
                log.error(e1.getMessage(), e1);
            }
            throw e;
        }
    }

    @Override
    public LockEngine getProjectsLockEngine() {
        return projectsLockEngine;
    }

    @Override
    public boolean isOpenedOtherProject(AProject project) {
        return getProjects(false).stream()
                .anyMatch(p -> p.isOpened() && project.getBusinessName()
                        .equals(p.getBusinessName()) && (!project.getRepository()
                        .getId()
                        .equals(p.getRepository().getId()) || !project.getRealPath().equals(p.getRealPath())));

    }
}
