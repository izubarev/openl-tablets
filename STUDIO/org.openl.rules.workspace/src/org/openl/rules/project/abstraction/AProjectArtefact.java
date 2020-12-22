package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.RuntimeExceptionWrapper;

public class AProjectArtefact implements IProjectArtefact {
    private final AProject project;
    private Repository repository;
    private FileData fileData;

    private final Date modifiedTime;

    public AProjectArtefact(AProject project, Repository repository, FileData fileData) {
        this.project = project;
        this.repository = repository;
        this.fileData = fileData;
        this.modifiedTime = fileData == null ? null : fileData.getModifiedAt();
    }

    public AProject getProject() {
        return project;
    }

    public FileData getFileData() {
        return fileData;
    }

    public void setFileData(FileData fileData) {
        this.fileData = fileData;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void delete() throws ProjectException {
        FileData data = getFileData();
        try {
            getRepository().delete(data);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFileData().getName());
    }

    public String getInternalPath() {
        String projectPath = getProject().getFileData().getName();
        return getFileData().getName().substring(projectPath.length() + 1);
    }

    public String getName() {
        String name = getFileData().getName();
        return name.substring(name.lastIndexOf('/') + 1);
    }

    public boolean isFolder() {
        return false;
    }

    // current version
    public ProjectVersion getVersion() {
        return createProjectVersion(getFileData());
    }

    public List<ProjectVersion> getVersions() {
        if (getFileData() == null) {
            return Collections.emptyList();
        }
        Collection<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        List<ProjectVersion> versions = new ArrayList<>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public int getVersionsCount() {
        try {
            return getFileData() == null ? 0 : getRepository().listHistory(getFileData().getName()).size();
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    protected ProjectVersion createProjectVersion(FileData fileData) {
        if (fileData == null) {
            return new RepositoryProjectVersionImpl();
        }
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(fileData.getModifiedAt(), fileData.getAuthor());
        String version = fileData.getVersion();
        RepositoryProjectVersionImpl projectVersion = new RepositoryProjectVersionImpl(version == null ? "0" : version,
            rvii,
            fileData.isDeleted());
        projectVersion.setVersionComment(fileData.getComment());
        return projectVersion;
    }

    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        refresh();
    }

    public void refresh() {
        //nothing to do
    }

    /**
     * Try to lock the project if it's not locked already. Does not overwrite lock info if the user was locked already.
     *
     * @return false if the project was locked by other user. true if project wasn't locked before or was locked by me.
     */
    public boolean tryLock() {
        // Default implementation does nothing and returns true to indicate that invocation was successful.
        return true;
    }

    /**
     * Try to lock the project if it's not locked already.
     * If the project was locked by other user, throws ProjectException.
     * @throws ProjectException if cannot lock the project, or the project was locked by other user.
     */
    public final void tryLockOrThrow() throws ProjectException {
        if (!tryLock()) {
            throw new ProjectException("The project is locked by other user");
        }
    }

    public void unlock() throws ProjectException {
        // Do nothing
    }

    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByUser(CommonUser user) {
        return isLockedByUser(getLockInfo(), user);
    }

    protected boolean isLockedByUser(LockInfo lockInfo, CommonUser user) {
        if (lockInfo.isLocked()) {
            String lockedBy = lockInfo.getLockedBy();
            return lockedBy.equals(user.getUserName()) || isLockedByDefaultUser(lockedBy, user);

        }
        return false;
    }

    public LockInfo getLockInfo() {
        return LockInfo.NO_LOCK;
    }

    public boolean isModified() {
        FileData data = getFileData();
        return data != null && (modifiedTime == null || !modifiedTime.equals(data.getModifiedAt()));
    }

    /**
     * For backward compatibility. Earlier user name in the single user mode analog was "LOCAL". Checks that lockedUser
     * is LOCAL and current user is DEFAULT
     *
     * @param lockedUser - owner of the lock
     * @param currentUser - current user trying to unlock
     * @return true if owner of the lock is "LOCAL" and current user is "DEFAULT"
     */
    private boolean isLockedByDefaultUser(String lockedUser, CommonUser currentUser) {
        return "LOCAL".equals(lockedUser) && "DEFAULT".equals(currentUser.getUserName());
    }

    public boolean isHistoric() {
        return getFileData().getVersion() != null;
    }

}
