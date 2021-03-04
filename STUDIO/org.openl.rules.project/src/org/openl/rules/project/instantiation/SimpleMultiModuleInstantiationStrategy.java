package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashSet;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * The simplest way of multimodule instantiation strategy. There will be created virtual module that depends on each
 * predefined module(means virtual module will have dependency for each module).
 *
 * @author PUdalau
 */
public class SimpleMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {

    private RulesEngineFactory<?> engineFactory;

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode) {
        super(modules, dependencyManager, classLoader, executionMode);
    }

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            boolean executionMode) {
        super(modules, dependencyManager, executionMode);
    }

    @Override
    public void reset() {
        super.reset();
        engineFactory = null;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to resolve an interface.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().newEngineInstance();
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to instantiate.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RulesEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = getServiceClass();
        if (engineFactory == null) {
            engineFactory = new RulesEngineFactory<>(createVirtualSourceCodeModule(), (Class<Object>) serviceClass);
            engineFactory.setExecutionMode(isExecutionMode());

            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<>();
            Collection<String> allExcludes = new HashSet<>();
            for (Module m : getModules()) {
                MethodFilter methodFilter = m.getMethodFilter();
                if (methodFilter != null) {
                    if (methodFilter.getIncludes() != null) {
                        allIncludes.addAll(methodFilter.getIncludes());
                    }
                    if (methodFilter.getExcludes() != null) {
                        allExcludes.addAll(methodFilter.getExcludes());
                    }
                }
            }
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                String[] includes = new String[] {};
                String[] excludes = new String[] {};
                includes = allIncludes.toArray(includes);
                excludes = allExcludes.toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGeneratorImpl(includes, excludes));
            }
            engineFactory.setDependencyManager(getDependencyManager());
        }

        return engineFactory;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (engineFactory != null) {
            engineFactory.setInterfaceClass((Class) serviceClass);
        }
    }
}