package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.filter.AllFilter;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProductionDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class ProductionRepositoriesTreeState {
    @Autowired
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    @Autowired
    private DeploymentManager deploymentManager;

    @Autowired
    private RepositoryFactoryProxy productionRepositoryFactoryProxy;

    @Autowired
    private PropertyResolver propertyResolver;

    private final Logger log = LoggerFactory.getLogger(ProductionRepositoriesTreeState.class);
    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;

    private final IFilter<AProjectArtefact> filter = new AllFilter<>();

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    private void buildTree() {
        if (root != null) {
            return;
        }

        log.debug("Starting buildTree()");

        root = new TreeRepository("", "", filter, "root");

        /* get list of production repos */
        for (RepositoryConfiguration repoConfig : getRepositories()) {
            String prName = repoConfig.getName();
            TreeRepository productionRepository = new TreeRepository(prName,
                prName,
                filter,
                UiConst.TYPE_PRODUCTION_REPOSITORY);
            productionRepository.setData(null);

            root.addChild(prName, productionRepository);

            /* Get repo's deployment configs */
            IFilter<AProjectArtefact> filter = this.filter;
            List<AProjectFolder> repoList = getPRepositoryProjects(repoConfig);
            repoList.sort(RepositoryUtils.ARTEFACT_COMPARATOR);

            for (AProjectFolder project : repoList) {
                TreeProductionDProject tpdp = new TreeProductionDProject("" + project.getName().hashCode(),
                    project.getName(),
                    filter);
                tpdp.setData(project);
                tpdp.setParent(productionRepository);
                productionRepository.add(tpdp);
            }

            if (repoList.isEmpty()) {
                // Initialize content of empty node
                productionRepository.getElements();
            }

        }

        log.debug("Finishing buildTree()");
    }

    private List<AProjectFolder> getPRepositoryProjects(RepositoryConfiguration repoConfig) {
        try {
            Repository repository = productionRepositoryFactoryProxy.getRepositoryInstance(repoConfig.getConfigName());
            String deploymentsPath = productionRepositoryFactoryProxy.getBasePath(repoConfig.getConfigName());
            return new ArrayList<>(DeployUtils.getLastDeploymentProjects(repository, deploymentsPath));
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    private Collection<RepositoryConfiguration> getRepositories() {
        List<RepositoryConfiguration> repos = new ArrayList<>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, propertyResolver);
            repos.add(config);
        }

        repos.sort(RepositoryConfiguration.COMPARATOR);

        return repos;
    }

    public TreeRepository getRoot() {
        // buildTree();
        return root;
    }

    public void initTree() {
        buildTree();
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
        repositorySelectNodeStateHolder.setSelectedNode((TreeNode) tree.getRowData());
        tree.setRowKey(storedKey);
    }

    public TreeNode getFirstProductionRepo() {
        try {
            String repoName = getRepositories().iterator().next().getName();
            return root.getElements().get(repoName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        root = null;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public RepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
    }

    public void setProductionRepositoryFactoryProxy(RepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public RepositorySelectNodeStateHolder getRepositorySelectNodeStateHolder() {
        return repositorySelectNodeStateHolder;
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }
}
