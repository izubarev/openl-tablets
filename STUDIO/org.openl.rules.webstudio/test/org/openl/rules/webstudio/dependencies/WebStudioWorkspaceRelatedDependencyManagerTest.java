package org.openl.rules.webstudio.dependencies;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;

public class WebStudioWorkspaceRelatedDependencyManagerTest {

    public static class WebStudioWorkspaceRelatedSimpleProjectEngineFactoryBuilder<T> extends SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<T> {
        @Override
        public WebStudioWorkspaceRelatedSimpleProjectEngineFactory<T> build() {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty.");
            }
            File projectFile = new File(project);
            File[] dependencies = getProjectDependencies();
            return new WebStudioWorkspaceRelatedSimpleProjectEngineFactory<>(projectFile,
                dependencies,
                classLoader,
                interfaceClass,
                externalParameters,
                provideRuntimeContext,
                provideVariations,
                executionMode);
        }
    }

    public static class WebStudioWorkspaceRelatedSimpleProjectEngineFactory<T> extends SimpleProjectEngineFactory<T> {
        public WebStudioWorkspaceRelatedSimpleProjectEngineFactory(File project,
                File[] projectDependencies,
                ClassLoader classLoader,
                Class<T> interfaceClass,
                Map<String, Object> externalParameters,
                boolean provideRuntimeContext,
                boolean provideVariations,
                boolean executionMode) {
            super(project,
                projectDependencies,
                classLoader,
                interfaceClass,
                externalParameters,
                provideRuntimeContext,
                provideVariations,
                executionMode);
        }

        @Override
        protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
            return new WebStudioWorkspaceRelatedDependencyManager(buildProjectDescriptors(),
                classLoader,
                isExecutionMode(),
                getExternalParameters());

        }
    }

    @Test
    public void test() throws ProjectResolvingException, OpenLCompilationException {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        final WebStudioWorkspaceRelatedSimpleProjectEngineFactory<?> factory = (WebStudioWorkspaceRelatedSimpleProjectEngineFactory<?>) (new WebStudioWorkspaceRelatedSimpleProjectEngineFactoryBuilder<>()
            .setProject("test/rules/compilation")
            .setExecutionMode(false)
            .build());

        WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceRelatedDependencyManager = (WebStudioWorkspaceRelatedDependencyManager) factory
            .getDependencyManager();
        Random rnd = new Random();

        for (int i = 0; i < 100; i++) {
            final int i0 = i;
            executorService.submit(() -> {
                if (i0 % 4 == 0) {
                    int p = (rnd.nextInt(3) + 1);
                    webStudioWorkspaceRelatedDependencyManager.reset(new Dependency(DependencyType.MODULE,
                        new IdentifierNode(DependencyType.MODULE.name(), null, "Module" + p, null)));
                    try {
                        webStudioWorkspaceRelatedDependencyManager.loadDependency(new Dependency(DependencyType.MODULE,
                            new IdentifierNode(DependencyType.MODULE.name(), null, "Module" + p, null)));
                    } catch (OpenLCompilationException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        webStudioWorkspaceRelatedDependencyManager
                            .loadDependencyAsync(
                                new Dependency(DependencyType.MODULE,
                                    new IdentifierNode(DependencyType.MODULE.name(),
                                        null,
                                        SimpleDependencyLoader.buildDependencyName(factory.getProjectDescriptor(),
                                            null),
                                        null)),
                                (e) -> {
                                    if (e.getCompiledOpenClass().hasErrors()) {
                                        System.out.println("Compiled Project with errors");
                                    } else {
                                        System.out.println("Compiled Project");
                                    }
                                });
                    } catch (ProjectResolvingException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        System.out.println("Compiled Module");

        try {
            Thread.sleep(100000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
