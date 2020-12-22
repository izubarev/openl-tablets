package org.openl.rules.webstudio.web.repository.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductionDProject extends TreeProductFolder {
    private final IFilter<AProjectArtefact> filter;

    public TreeProductionDProject(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name, filter);
        this.filter = filter;
    }

    private Map<Object, TreeNode> elements;

    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_DEPLOYMENT_PROJECT;
    }

    @Override
    public String getIconLeaf() {
        return UiConst.ICON_PROJECT_CLOSED;
    }

    @Override
    public Map<Object, TreeNode> getElements() {
        if (elements == null && !isLeafOnly()) {
            elements = new LinkedHashMap<>();

            Collection<IProject> prjList = ((Deployment) getData()).getProjects();
            AProjectArtefact[] sortedArtefacts = new AProjectArtefact[prjList.size()];
            int i = 0;
            for (IProject iProject : prjList) {
                sortedArtefacts[i++] = (AProjectArtefact) iProject;
            }

            Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);

            for (AProjectArtefact apa : sortedArtefacts) {
                addChild(apa);
            }

        }
        return elements;
    }

    @Override
    public boolean isLeaf() {
        // If elements aren't initialized, consider it as not leaf
        return isLeafOnly() || elements != null && elements.isEmpty();
    }

    @Override
    public void addChild(AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = RepositoryUtils.getTreeNodeId(childArtefact);
        if (childArtefact instanceof AProjectFolder) {
            TreeProductProject prj = new TreeProductProject(id, childArtefact.getName(), filter);
            prj.setData(childArtefact);

            add(prj);
        }
    }

    public Date getModifiedAt() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedAt() : null;
    }

    public String getModifiedBy() {
        ProjectVersion projectVersion = getProject().getVersion();
        /* zero */
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedBy() : null;
    }

    private AProjectFolder getProject() {
        return (AProjectFolder) getData();
    }

}
