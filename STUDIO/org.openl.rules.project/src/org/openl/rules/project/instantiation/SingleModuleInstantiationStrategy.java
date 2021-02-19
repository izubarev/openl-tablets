package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Instantiation strategy for single module.
 *
 * @author PUdalau
 */
public abstract class SingleModuleInstantiationStrategy extends CommonRulesInstantiationStrategy {

    /**
     * Root <code>Module</code> that is used as start point for Openl compilation.
     */
    private final Module module;

    public SingleModuleInstantiationStrategy(Module module,
            IDependencyManager dependencyManager,
            boolean executionMode) {
        this(module, dependencyManager, null, executionMode);
    }

    public SingleModuleInstantiationStrategy(Module module,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode) {
        super(executionMode, dependencyManager, classLoader);
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    // Single module strategy does not compile dependencies. Exception not required.
    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }
        return classLoader;
    }

    @Override
    protected ClassLoader initClassLoader() {
        ProjectDescriptor project = getModule().getProject();
        return new OpenLClassLoader(project.getClassPathUrls(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Collection<Module> getModules() {
        return Collections.singleton(getModule());
    }

    protected Map<String, Object> prepareExternalParameters() {
        Map<String, Object> externalProperties = new HashMap<>();
        if (getModule().getProperties() != null) {
            externalProperties.putAll(getModule().getProperties());
        }
        if (getExternalParameters() != null) {
            externalProperties.putAll(getExternalParameters());
        }
        return externalProperties;
    }
}
