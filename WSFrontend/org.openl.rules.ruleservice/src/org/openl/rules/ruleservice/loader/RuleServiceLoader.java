package org.openl.rules.ruleservice.loader;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.model.Module;

/**
 * Wrapper on data source that gives access to data source and resolves the OpenL projects/modules inside the projects.
 * Contains own storage for all projects that is used in services.
 *
 * @author Marat Kamalov
 */
public interface RuleServiceLoader {

    /**
     * Adds a lister to data source.
     *
     * @param dataSourceListener
     */
    void setListener(DataSourceListener dataSourceListener);

    /**
     * Returns deployments
     *
     * @return list of deployments.
     */
    Collection<IDeployment> getDeployments();

    /**
     * @param deploymentName
     * @param deploymentVersion
     * @param projectName
     * @return
     */
    Collection<Module> resolveModulesForProject(String deploymentName,
            CommonVersion deploymentVersion,
            String projectName);

    /**
     * Gets a deployment from data source.
     *
     * @param deploymentName target deployment name
     * @param deploymentVersion target version
     * @return deployment
     */
    IDeployment getDeployment(String deploymentName, CommonVersion deploymentVersion);

    /**
     * Removes deploy path from folder path
     *
     * @param realFolderPath real path to folder
     * @return cleared folder path
     */
    String getLogicalProjectFolder(String realFolderPath);
}
