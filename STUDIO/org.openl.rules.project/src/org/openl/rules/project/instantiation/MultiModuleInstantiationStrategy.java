package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IDependency;
import org.openl.util.IOUtils;

/**
 * Instantiation strategy that combines several modules into single rules module.
 * <p/>
 * Note: it works only in execution mode.
 *
 * @author PUdalau
 */
public abstract class MultiModuleInstantiationStrategy extends CommonRulesInstantiationStrategy {

    private final Collection<Module> modules;

    public MultiModuleInstantiationStrategy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            boolean executionMode) {
        this(modules, dependencyManager, null, executionMode);
    }

    public MultiModuleInstantiationStrategy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode) {
        // multimodule is only available for execution(execution mode == true)
        super(executionMode, dependencyManager, classLoader);
        this.modules = modules;
    }

    @Override
    public Collection<Module> getModules() {
        return modules;
    }

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException {
        OpenLClassLoader classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        try {
            Set<ProjectDescriptor> projectDescriptors = modules.stream()
                .map(Module::getProject)
                .collect(Collectors.toSet());
            for (ProjectDescriptor pd : projectDescriptors) {
                try {
                    CompiledDependency compiledDependency = getDependencyManager()
                        .loadDependency(AbstractDependencyManager.buildResolvedDependency(pd));
                    CompiledOpenClass compiledOpenClass = compiledDependency.getCompiledOpenClass();
                    classLoader.addClassLoader(compiledOpenClass.getClassLoader());
                } catch (OpenLCompilationException e) {
                    throw new RulesInstantiationException(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // If exception is thrown, we must close classLoader in this method and rethrow exception.
            // If no exception, classLoader will be closed later.
            IOUtils.closeQuietly(classLoader);
            throw e;
        }
        return classLoader;
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with dependencies on all modules.
     */
    protected IOpenSourceCodeModule createVirtualSourceCodeModule() {
        List<IDependency> dependencies = getModules().stream()
            .map(AbstractDependencyManager::buildResolvedDependency)
            .distinct()
            .collect(Collectors.toList());
        Map<String, Object> params = new HashMap<>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        if (params.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params
                .get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY);
            dependencies.addAll(externalDependencies);
        }
        params.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

}
