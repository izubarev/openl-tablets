package org.openl.rules.project.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnhancerHelper;
import org.openl.validation.ValidatedCompiledOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServiceInterfaceProjectValidator implements ProjectValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceInterfaceProjectValidator.class);

    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private boolean provideRuntimeContext = true;
    private boolean provideVariations;

    private final IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private RulesDeploy rulesDeploy;
    private ClassLoader classLoader;

    protected ProjectResource loadProjectResource(ProjectResourceLoader projectResourceLoader,
            ProjectDescriptor projectDescriptor,
            String name) {
        ProjectResource[] projectResources = projectResourceLoader.loadResource(name);
        return Arrays.stream(projectResources)
            .filter(e -> Objects.equals(e.getProjectDescriptor().getName(), projectDescriptor.getName()))
            .findFirst()
            .orElse(null);
    }

    protected RulesDeploy loadRulesDeploy(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = loadProjectResource(projectResourceLoader,
            projectDescriptor,
            RULES_DEPLOY_XML);
        if (projectResource != null) {
            try {
                return rulesDeploySerializer.deserialize(new FileInputStream(new File(projectResource.getFile())));
            } catch (FileNotFoundException e) {
                LOG.debug("Ignored error: ", e);
                return null;
            }
        }
        return null;
    }

    protected RulesDeploy getRulesDeploy(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        if (rulesDeploy == null) {
            rulesDeploy = loadRulesDeploy(projectDescriptor, compiledOpenClass);
        }
        return rulesDeploy;
    }

    protected ClassLoader resolveServiceClassLoader(
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException {
        if (classLoader == null) {
            ClassLoader moduleGeneratedClassesClassLoader = ((XlsModuleOpenClass) instantiationStrategy.compile()
                .getOpenClassWithErrors()).getClassGenerationClassLoader();
            OpenLBundleClassLoader openLBundleClassLoader = new OpenLBundleClassLoader(null);
            openLBundleClassLoader.addClassLoader(moduleGeneratedClassesClassLoader);
            openLBundleClassLoader.addClassLoader(instantiationStrategy.getClassLoader());
            classLoader = openLBundleClassLoader;
        }
        return classLoader;
    }

    protected Class<?> resolveInterface(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy,
            ValidatedCompiledOpenClass validatedCompiledOpenClass) throws RulesInstantiationException {
        RulesDeploy rulesDeployValue = getRulesDeploy(projectDescriptor, validatedCompiledOpenClass);
        if (rulesDeployValue != null && rulesDeployValue.getServiceName() != null) {
            final String serviceClassName = rulesDeployValue.getServiceClass().trim();
            if (!StringUtils.isEmpty(serviceClassName)) {
                try {
                    return validatedCompiledOpenClass.getClassLoader().loadClass(serviceClassName);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    validatedCompiledOpenClass.addMessage(
                        OpenLMessagesUtils.newWarnMessage(String.format("Failed to load a service class '%s'.%s",
                            serviceClassName,
                            StringUtils.isNotBlank(e.getMessage()) ? " " + e.getMessage() : StringUtils.EMPTY)));
                }
            }
        }
        final boolean hasContext = (rulesDeployValue == null && isProvideRuntimeContext()) || (rulesDeployValue != null && Boolean.TRUE
            .equals(rulesDeployValue.isProvideRuntimeContext()));
        final boolean hasVariations = (rulesDeployValue == null && isProvideVariations()) || (rulesDeployValue != null && Boolean.TRUE
            .equals(rulesDeployValue.isProvideVariations()));
        if (hasVariations) {
            rulesInstantiationStrategy = new VariationInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        if (hasContext) {
            rulesInstantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        String annotationTemplateClassName = null;
        if (rulesDeployValue != null) {
            annotationTemplateClassName = rulesDeployValue.getAnnotationTemplateClassName() != null ? rulesDeployValue
                .getAnnotationTemplateClassName() : rulesDeployValue.getInterceptingTemplateClassName();
            if (annotationTemplateClassName != null) {
                annotationTemplateClassName = annotationTemplateClassName.trim();
            }
        }
        Class<?> serviceClass = rulesInstantiationStrategy.getInstanceClass();
        ClassLoader resolveServiceClassLoader = resolveServiceClassLoader(rulesInstantiationStrategy);
        if (!StringUtils.isEmpty(annotationTemplateClassName)) {
            try {
                Class<?> annotationTemplateClass = resolveServiceClassLoader.loadClass(annotationTemplateClassName);
                if (annotationTemplateClass.isInterface()) {
                    serviceClass = DynamicInterfaceAnnotationEnhancerHelper.decorate(serviceClass,
                        annotationTemplateClass,
                        rulesInstantiationStrategy.compile().getOpenClassWithErrors(),
                        resolveServiceClassLoader);
                } else {
                    validatedCompiledOpenClass.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                        "Failed to apply annotation template class '%s'. Interface is expected, but class is found.",
                        annotationTemplateClassName)));
                }
            } catch (Exception | NoClassDefFoundError e) {
                validatedCompiledOpenClass.addMessage(OpenLMessagesUtils
                    .newWarnMessage(String.format("Failed to load or apply annotation template class '%s'.%s",
                        annotationTemplateClassName,
                        StringUtils.isNotBlank(e.getMessage()) ? " " + e.getMessage() : StringUtils.EMPTY)));
            }
        }
        return RuleServiceInstantiationFactoryHelper.buildInterfaceForService(rulesInstantiationStrategy.compile()
            .getOpenClassWithErrors(), serviceClass, resolveServiceClassLoader, hasContext, hasVariations);
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public boolean isProvideVariations() {
        return provideVariations;
    }

    public void setProvideVariations(boolean provideVariations) {
        this.provideVariations = provideVariations;
    }
}
