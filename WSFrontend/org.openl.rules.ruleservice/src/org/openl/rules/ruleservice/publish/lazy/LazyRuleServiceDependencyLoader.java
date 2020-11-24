package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager.DependencyCompilationType;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LazyRuleServiceDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(LazyRuleServiceDependencyLoader.class);

    private final RuleServiceDependencyManager dependencyManager;
    private final String dependencyName;
    private final boolean realCompileRequired;
    private CompiledOpenClass lazyCompiledOpenClass;
    private final DeploymentDescription deployment;
    private final ProjectDescriptor project;
    private final Module module;

    public LazyRuleServiceDependencyLoader(DeploymentDescription deployment,
            ProjectDescriptor project,
            Module module,
            boolean realCompileRequired,
            RuleServiceDependencyManager dependencyManager) {
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot null.");
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.realCompileRequired = realCompileRequired;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependencyName = SimpleDependencyLoader.buildDependencyName(project, module);
    }

    @Override
    public CompiledDependency getRefToCompiledDependency() {
        return lazyCompiledDependency;
    }

    private Collection<Module> getModules() {
        return module != null ? Collections.singleton(module) : project.getModules();
    }

    @Override
    public boolean isProject() {
        return module == null;
    }

    @Override
    public ProjectDescriptor getProject() {
        return project;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    private ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        return dependencyManager.getExternalJarsClassLoader(getProject());
    }

    public CompiledOpenClass compile(final String dependencyName,
            final RuleServiceDependencyManager dependencyManager) throws OpenLCompilationException {
        if (lazyCompiledOpenClass != null) {
            return lazyCompiledOpenClass;
        }

        log.debug("Compiling lazy dependency: deployment='{}', version='{}', name='{}'.",
            deployment.getName(),
            deployment.getVersion().getVersionName(),
            dependencyName);

        final ClassLoader classLoader = buildClassLoader(dependencyManager);
        RulesInstantiationStrategy rulesInstantiationStrategy;
        Collection<Module> modules = getModules();
        if (isProject()) {
            if (modules.isEmpty()) {
                throw new IllegalStateException("Expected at least one module in the project.");
            }
            rulesInstantiationStrategy = new LazyInstantiationStrategy(deployment,
                modules,
                dependencyManager,
                classLoader);
        } else {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(module, true, dependencyManager, classLoader);
        }
        rulesInstantiationStrategy.setServiceClass(LazyRuleServiceDependencyLoaderInterface.class);// Prevent
        // generation interface and Virtual module duplicate (instantiate method). Improve performance.
        final Map<String, Object> parameters = ProjectExternalDependenciesHelper
            .buildExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), modules);
        rulesInstantiationStrategy.setExternalParameters(parameters);
        IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
        try {
            LazyBinderMethodHandler
                .setPrebindHandler(new LazyPrebindHandler(modules, dependencyManager, classLoader, deployment));
            try {
                dependencyManager.compilationBegin();
                lazyCompiledOpenClass = rulesInstantiationStrategy.compile();
                if (!isProject() && realCompileRequired) {
                    synchronized (lazyCompiledOpenClass) {
                        CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance()
                            .get(deployment, dependencyName);
                        if (compiledOpenClass == null) {
                            CompiledOpenClassCache.compileToCache(dependencyManager,
                                dependencyName,
                                deployment,
                                modules.iterator().next(),
                                classLoader);
                        }
                    }
                }
                dependencyManager.compilationCompleted(this,
                    realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                    !lazyCompiledOpenClass.hasErrors());
            } finally {
                if (lazyCompiledOpenClass == null) {
                    dependencyManager.compilationCompleted(this,
                        realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                        false);
                }
            }
            return lazyCompiledOpenClass;
        } catch (Exception ex) {
            throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName), ex);
        } finally {
            LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
        }
    }

    private boolean isCompiledBefore = false;
    private CompiledDependency lazyCompiledDependency = null;

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        if (!isCompiledBefore) {
            compile(dependencyName, dependencyManager);
            isCompiledBefore = true;
        }
        if (lazyCompiledDependency == null) {
            CompiledOpenClass compiledOpenClass = new LazyCompiledOpenClass(dependencyManager,
                this,
                new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, dependencyName, null)));
            lazyCompiledDependency = new CompiledDependency(dependencyName, compiledOpenClass);
        }
        return lazyCompiledDependency;
    }

    @Override
    public void reset() {
        CompiledOpenClassCache.getInstance().removeAll(deployment);
    }

    interface LazyRuleServiceDependencyLoaderInterface {
    }

}