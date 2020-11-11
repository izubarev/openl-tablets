package org.openl.rules.webstudio.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.openl.types.NullOpenClass;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {

    private static final ExecutorService executorService = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final List<ProjectDescriptor> projects;
    private final AtomicLong version = new AtomicLong(0);
    private final ThreadLocal<Long> threadVersion = new ThreadLocal<>();

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = new ArrayList<>(Objects.requireNonNull(projects, "projects cannot be null"));
        initDependencyLoaders();
        executorService.submit(this::compileAll);
    }

    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        Long currentThreadVersion = threadVersion.get();
        if (currentThreadVersion == null) {
            threadVersion.set(version.get());
            try {
                return super.loadDependency(dependency);
            } finally {
                threadVersion.remove();
            }
        } else {
            if (Objects.equals(currentThreadVersion, version.get())) {
                return super.loadDependency(dependency);
            } else {
                return new CompiledDependency(dependency.getNode().getIdentifier(),
                    new CompiledOpenClass(NullOpenClass.the,
                        Collections.singletonList(
                            new OpenLMessage("Rules compilation is interrupted by another thread.", Severity.ERROR))));
            }
        }
    }

    public ThreadLocal<Long> getThreadVersion() {
        return threadVersion;
    }

    public AtomicLong getVersion() {
        return version;
    }

    public CompiledDependency getDependency(IDependency dependency) throws OpenLCompilationException {
        final IDependencyLoader dependencyLoader = findDependencyLoader(dependency);
        return dependencyLoader.isCompiled() ? dependencyLoader.getCompiledDependency() : null;
    }

    private void compileAll() {
        getDependencyLoaders().forEach((k, v) -> v.forEach(e -> {
            try {
                e.getCompiledDependency();
            } catch (OpenLCompilationException openLCompilationException) {
                openLCompilationException.printStackTrace();
            }
        }));
    }

    protected Map<String, Collection<IDependencyLoader>> initDependencyLoaders() {
        Map<String, Collection<IDependencyLoader>> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        WebStudioDependencyLoader moduleDependencyLoader = new WebStudioDependencyLoader(project,
                            m,
                            this);
                        Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                            .computeIfAbsent(moduleDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                        dependencyLoadersByName.add(moduleDependencyLoader);
                    }
                }

                WebStudioDependencyLoader projectDependencyLoader = new WebStudioDependencyLoader(project, null, this);
                Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                    .computeIfAbsent(projectDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                dependencyLoadersByName.add(projectDependencyLoader);

            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }

    @Override
    public void reset(IDependency dependency) {
        version.incrementAndGet();
        super.reset(dependency);
        executorService.submit(this::compileAll);
    }

    @Override
    public void resetAll() {
        version.incrementAndGet();
        super.resetAll();
        executorService.submit(this::compileAll);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
