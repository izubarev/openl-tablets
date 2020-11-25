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
import java.util.function.Consumer;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLErrorMessage;
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

    public WebStudioWorkspaceRelatedDependencyManager(Collection<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = new ArrayList<>(Objects.requireNonNull(projects, "projects cannot be null"));
        initDependencyLoaders();
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
                        Collections.singletonList(new CompilationInterruptedOpenLErrorMessage())));
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
        return dependencyLoader.getRefToCompiledDependency();
    }

    public void loadDependencyAsync(IDependency dependency, Consumer<CompiledDependency> consumer) {
        executorService.submit(() -> {
            CompiledDependency compiledDependency;
            try {
                compiledDependency = this.loadDependency(dependency);
            } catch (OpenLCompilationException e) {
                compiledDependency = new CompiledDependency(dependency.getNode().getIdentifier(),
                    new CompiledOpenClass(NullOpenClass.the, Collections.singletonList(new OpenLErrorMessage(e))));
            }
            if (compiledDependency.getCompiledOpenClass()
                .getMessages()
                .stream()
                .anyMatch(e -> e instanceof CompilationInterruptedOpenLErrorMessage)) {
                loadDependencyAsync(dependency, consumer);
            } else {
                consumer.accept(compiledDependency);
            }
        });
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
    }

    @Override
    public void resetAll() {
        version.incrementAndGet();
        super.resetAll();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
