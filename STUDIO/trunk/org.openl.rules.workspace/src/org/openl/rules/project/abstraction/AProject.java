package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectDependency.ProjectDependencyHelper;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ArtefactProperties;

public class AProject extends AProjectFolder {
    public AProject(FolderAPI api) {
        super(api, null);
    }

    @Override
    public AProject getProject() {
        return this;
    }

    public List<ProjectDependency> getDependencies() {
        List<ProjectDependency> dependencies = new ArrayList<ProjectDependency>();
        if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
            InputStream content = null;
            try {
                content = ((AProjectResource) getArtefact(ArtefactProperties.DEPENDENCIES_FILE)).getContent();
                dependencies = ProjectDependencyHelper.deserialize(content);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(content);
            }
        }

        return dependencies;
    }

    public void setDependencies(List<ProjectDependency> dependencies) throws ProjectException {
        if (CollectionUtils.isEmpty(dependencies)) {
            if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
                getArtefact(ArtefactProperties.DEPENDENCIES_FILE).delete();
            }
        } else {
            String dependenciesAsString = ProjectDependencyHelper.serialize(dependencies);
            try {
                if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
                    ((AProjectResource) getArtefact(ArtefactProperties.DEPENDENCIES_FILE))
                            .setContent(new ByteArrayInputStream(dependenciesAsString.getBytes("UTF-8")));
                } else {
                    addResource(ArtefactProperties.DEPENDENCIES_FILE,
                            new ByteArrayInputStream(dependenciesAsString.getBytes("UTF-8")));
                }
            } catch (Exception e) {
                // TODO
                e.printStackTrace();
            }
        }
    }

    @Override
    public void delete() throws ProjectException {
        throw new ProjectException("Unsupported operation.");
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isLocked() && !isLockedByUser(user)) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user!", null,
                    getName());
        }

        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        try {
            addProperty(new PropertyImpl(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION, ValueType.BOOLEAN, true));
        } catch (PropertyException e) {
            throw new ProjectException("Failed to mark project as deleted.", e);
        }
    }

    public void checkIn(CommonUser user) throws ProjectException {
        ProjectVersion currentVersion = getLastVersion();
        checkIn(user, currentVersion.getMajor(), currentVersion.getMinor());
    }

    public void checkIn(CommonUser user, int major, int minor) throws ProjectException {
        save(user, major, minor);
        unlock(user);
        refresh();
    }

    public void checkOut(CommonUser user) throws ProjectException {
        lock(user);
    }

    public void close(CommonUser user) throws ProjectException {
        if (isLockedByUser(user)) {
            unlock(user);
        }
        refresh();
    }

    public void erase(CommonUser user) throws ProjectException {
        getAPI().delete(user);
    }

    public boolean isDeleted() {
        return getAPI().hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
    }

    public void undelete() throws ProjectException {
        if (!isDeleted()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }

        try {
            removeProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
        } catch (PropertyException e) {
            throw new ProjectException("Failed to undelete project.", e);
        }
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        AProject project = (AProject) artefact;
        setDependencies(project.getDependencies());
        super.update(artefact, user, major, minor);
    }
    
    @Override
    public void smartUpdate(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        if (artefact.isModified()) {
            AProject project = (AProject) artefact;
            setDependencies(project.getDependencies());
            super.smartUpdate(artefact, user, major, minor);
        }
    }
    
    public AProject getProjectVersion(CommonVersion version) throws ProjectException{
        return new AProject(getAPI().getVersion(version));
    }
}
