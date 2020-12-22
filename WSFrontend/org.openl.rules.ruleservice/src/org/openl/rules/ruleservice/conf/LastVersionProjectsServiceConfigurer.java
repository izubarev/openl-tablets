package org.openl.rules.ruleservice.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.abstraction.IProjectArtefact;
import org.openl.rules.project.abstraction.IProjectResource;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ResourceLoader;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the latest deployments and deploys each of their projects as single service.
 *
 * @author PUdalau, Marat Kamalov
 */
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer {

    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Logger log = LoggerFactory.getLogger(LastVersionProjectsServiceConfigurer.class);

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
    private boolean provideRuntimeContext = false;
    private boolean supportVariations = false;
    private String supportedGroups = null;
    private DeploymentNameMatcher deploymentMatcher = DeploymentNameMatcher.DEFAULT;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader) {
        log.debug("Calculate services to be deployed...");

        Collection<IDeployment> deployments = ruleServiceLoader.getDeployments();

        Collection<ServiceDescription> serviceDescriptions = new HashSet<>();
        Set<String> serviceURLs = new HashSet<>();
        for (IDeployment deployment : deployments) {
            if (!deploymentMatcher.hasMatches(deployment.getDeploymentName())) {
                continue;
            }
            String deploymentName = deployment.getDeploymentName();
            CommonVersion deploymentVersion = deployment.getCommonVersion();
            DeploymentDescription deploymentDescription = new DeploymentDescription(deploymentName, deploymentVersion);
            for (IProject project : deployment.getProjects()) {
                if (project.isDeleted()) {
                    continue;
                }
                String projectName = project.getName();
                try {
                    Collection<Module> modulesOfProject = ruleServiceLoader
                        .resolveModulesForProject(deploymentName, deploymentVersion, projectName);
                    ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                        .setProvideRuntimeContext(isProvideRuntimeContext())
                        .setProvideVariations(isSupportVariations())
                        .setDeployment(deploymentDescription);

                    serviceDescriptionBuilder.setModules(modulesOfProject);
                    ResourceLoader resourceLoader = new ResourceLoaderImpl(project);
                    serviceDescriptionBuilder.setResourceLoader(resourceLoader);
                    if (!modulesOfProject.isEmpty()) {
                        RulesDeploy rulesDeploy = null;
                        try {
                            IProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                            if (artifact instanceof IProjectResource) {
                                IProjectResource resource = (IProjectResource) artifact;
                                try (InputStream content = resource.getContent()) {
                                    rulesDeploy = getRulesDeploySerializer().deserialize(content);
                                    serviceDescriptionBuilder.setRulesDeploy(rulesDeploy);
                                    if (rulesDeploy
                                        .getServiceClass() != null && !rulesDeploy.getServiceClass().trim().isEmpty()) {
                                        serviceDescriptionBuilder
                                            .setServiceClassName(rulesDeploy.getServiceClass().trim());
                                    }
                                    if (rulesDeploy.getRmiServiceClass() != null && !rulesDeploy.getRmiServiceClass()
                                        .trim()
                                        .isEmpty()) {
                                        serviceDescriptionBuilder
                                            .setRmiServiceClassName(rulesDeploy.getRmiServiceClass().trim());
                                    }
                                    if (rulesDeploy.isProvideRuntimeContext() != null) {
                                        serviceDescriptionBuilder
                                            .setProvideRuntimeContext(rulesDeploy.isProvideRuntimeContext());
                                    }
                                    if (rulesDeploy.isProvideVariations() != null) {
                                        serviceDescriptionBuilder
                                            .setProvideVariations(rulesDeploy.isProvideVariations());
                                    }
                                    if (rulesDeploy.getPublishers() != null) {
                                        for (RulesDeploy.PublisherType publisher : rulesDeploy.getPublishers()) {
                                            serviceDescriptionBuilder.addPublisher(publisher.toString());
                                        }
                                    }
                                    if (rulesDeploy.getConfiguration() != null) {
                                        serviceDescriptionBuilder.setConfiguration(rulesDeploy.getConfiguration());
                                    }
                                    if (rulesDeploy.getInterceptingTemplateClassName() != null && !rulesDeploy
                                        .getInterceptingTemplateClassName()
                                        .trim()
                                        .isEmpty()) {
                                        serviceDescriptionBuilder.setAnnotationTemplateClassName(
                                            rulesDeploy.getInterceptingTemplateClassName().trim());
                                    }
                                    if (rulesDeploy
                                        .getRmiName() != null && !rulesDeploy.getRmiName().trim().isEmpty()) {
                                        serviceDescriptionBuilder.setRmiName(rulesDeploy.getRmiName());
                                    }
                                    if (rulesDeploy.getAnnotationTemplateClassName() != null && !rulesDeploy
                                        .getAnnotationTemplateClassName()
                                        .trim()
                                        .isEmpty()) {
                                        serviceDescriptionBuilder.setAnnotationTemplateClassName(
                                            rulesDeploy.getAnnotationTemplateClassName().trim());
                                    }
                                }
                            }
                        } catch (ProjectException ignored) {
                        }

                        serviceDescriptionBuilder.setManifest(readManifestFile(project));
                        serviceDescriptionBuilder.setName(buildServiceName(deployment, projectName, rulesDeploy));
                        serviceDescriptionBuilder.setUrl(buildServiceUrl(deployment, projectName, rulesDeploy));
                        serviceDescriptionBuilder.setServicePath(project.getFolderPath());
                        ServiceDescription serviceDescription = serviceDescriptionBuilder.build();

                        if (!serviceDescriptions.contains(serviceDescription) && !serviceURLs
                            .contains(serviceDescription.getUrl()) && serviceGroupSupported(rulesDeploy)) {
                            serviceURLs.add(serviceDescription.getUrl());
                            serviceDescriptions.add(serviceDescription);
                        } else {
                            if (serviceDescriptions.contains(serviceDescription)) {
                                log.warn(
                                    "Service '{}' already exists in the deployment list. The second service has been skipped. Please, use unique name for services.",
                                    serviceDescription.getName());
                            }
                            if (serviceURLs.contains(serviceDescription.getUrl())) {
                                log.warn(
                                    "URL '{}' has already been registered. The second service has been skipped. Please, use unique URLs for services.",
                                    serviceDescription.getUrl());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(
                        "Failed to load a project from the repository. Project '{}' in deployment '{}' has been skipped.",
                        projectName,
                        deploymentName,
                        e);
                }
            }
        }

        return serviceDescriptions;
    }

    private Manifest readManifestFile(IProject project) {
        try {
            IProjectArtefact artifact = project.getArtefact(JarFile.MANIFEST_NAME);
            if (artifact instanceof IProjectResource) {
                try (InputStream content = ((IProjectResource) artifact).getContent()) {
                    return new Manifest(content);
                }
            }
        } catch (IOException | ProjectException ignored) {
        }
        return null;
    }

    private Set<String> getSupportedGroupsSet() {
        if (getSupportedGroups() != null && !getSupportedGroups().trim().isEmpty()) {
            String[] groups = getSupportedGroups().split(",");
            Set<String> supportedGroupSet = new HashSet<>();
            for (String group : groups) {
                supportedGroupSet.add(group.trim());
            }
            return supportedGroupSet;
        }
        return Collections.emptySet();
    }

    private boolean serviceGroupSupported(RulesDeploy rulesDeploy) {
        Set<String> supportedGroupSet = getSupportedGroupsSet();
        if (!supportedGroupSet.isEmpty()) {
            if (rulesDeploy == null || rulesDeploy.getGroups() == null || rulesDeploy.getGroups().trim().isEmpty()) {
                return false;
            }
            String[] groups = rulesDeploy.getGroups().split(",");
            for (String group : groups) {
                if (supportedGroupSet.contains(group)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private String buildServiceName(IDeployment deployment, String projectName, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (StringUtils.isNotEmpty(rulesDeploy.getServiceName())) {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return rulesDeploy.getServiceName() + "(version=" + rulesDeploy.getVersion() + ")";
                } else {
                    return rulesDeploy.getServiceName();
                }
            } else {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return deployment.getDeploymentName() + '_' + projectName + "(version=" + rulesDeploy
                        .getVersion() + ")";
                }
            }
        }
        return deployment.getDeploymentName() + '_' + projectName;
    }

    private String buildServiceUrl(IDeployment deployment, String projectName, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (StringUtils.isNotEmpty(rulesDeploy.getUrl())) {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    if (rulesDeploy.getUrl().startsWith("/")) {
                        return "/" + rulesDeploy.getVersion() + rulesDeploy.getUrl();
                    } else {
                        return "/" + rulesDeploy.getVersion() + "/" + rulesDeploy.getUrl();
                    }
                } else {
                    return rulesDeploy.getUrl();
                }
            } else {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return "/" + rulesDeploy.getVersion() + "/" + deployment.getDeploymentName() + '/' + projectName;
                }
            }
        }
        return deployment.getDeploymentName() + '/' + projectName;
    }

    public final IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    public final void setRulesDeploySerializer(IRulesDeploySerializer rulesDeploySerializer) {
        this.rulesDeploySerializer = Objects.requireNonNull(rulesDeploySerializer,
            "rulesDeploySerializer cannot be null");
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public boolean isSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
    }

    public void setSupportedGroups(String supportedGroups) {
        this.supportedGroups = supportedGroups;
    }

    public String getSupportedGroups() {
        return supportedGroups;
    }

    public void setDatasourceDeploymentPatterns(String deploymentPatterns) {
        this.deploymentMatcher = new DeploymentNameMatcher(deploymentPatterns);
    }

}
