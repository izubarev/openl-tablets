package org.openl.rules.project.instantiation;

import java.util.*;
import java.util.stream.Collectors;

import org.openl.OpenClassUtil;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyManager implements IDependencyManager {

    private final Logger log = LoggerFactory.getLogger(AbstractDependencyManager.class);

    private volatile Map<String, Collection<IDependencyLoader>> dependencyLoaders;
    private final LinkedHashSet<DependencyReference> dependencyReferences = new LinkedHashSet<>();
    private final ThreadLocal<Deque<String>> compilationStackThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<String, ClassLoader> externalJarsClassloaders = new HashMap<>();
    private final ClassLoader rootClassLoader;
    protected boolean executionMode;
    private Map<String, Object> externalParameters;

    public static class DependencyReference {
        String reference;
        String dependency;

        public DependencyReference(String dependency, String reference) {
            this.dependency = dependency;
            this.reference = reference;
        }

        public String getDependency() {
            return dependency;
        }

        public String getReference() {
            return reference;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + (reference == null ? 0 : reference.hashCode());
            result = 31 * result + (dependency == null ? 0 : dependency.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DependencyReference other = (DependencyReference) obj;
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (dependency == null) {
                return other.dependency == null;
            } else {
                return dependency.equals(other.dependency);
            }
        }

        @Override
        public String toString() {
            return String.format("DependencyReference [reference=%s, dependency=%s]", reference, dependency);
        }

    }

    protected AbstractDependencyManager(ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        this.rootClassLoader = rootClassLoader;
        this.executionMode = executionMode;
        this.externalParameters = new HashMap<>();
        this.externalParameters.put(XlsBinder.DISABLED_CLEAN_UP, Boolean.TRUE);
        if (externalParameters != null) {
            this.externalParameters.putAll(externalParameters);
        }
        this.externalParameters = Collections.unmodifiableMap(this.externalParameters);
    }

    public final Map<String, Collection<IDependencyLoader>> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            synchronized (this) {
                if (dependencyLoaders == null) {
                    dependencyLoaders = initDependencyLoaders();
                }
            }
        }
        return dependencyLoaders;
    }

    protected abstract Map<String, Collection<IDependencyLoader>> initDependencyLoaders();

    private Deque<String> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    @Override
    public Collection<String> getAllDependencies() {
        return Collections.unmodifiableSet(getDependencyLoaders().keySet());
    }

    @Override
    public final Collection<String> getAvailableDependencies() {
        Set<String> availableDependencies = new HashSet<>();
        for (Map.Entry<String, Collection<IDependencyLoader>> entry : getDependencyLoaders().entrySet()) {
            if (entry.getValue().stream().noneMatch(IDependencyLoader::isProject)) {
                availableDependencies.add(entry.getKey());
            }
        }
        return availableDependencies;
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        final IDependencyLoader dependencyLoader = findDependencyLoader(dependency);
        final String dependencyName = dependency.getNode().getIdentifier();
        Deque<String> compilationStack = getCompilationStack();
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    compilationStack
                        .contains(dependencyName) ? "Dependency '{}' in the compilation stack."
                                                  : "Dependency '{}' is not found in the compilation stack.",
                    dependencyName);
            }
            boolean isCircularDependency = !dependencyLoader.isProject() && compilationStack.contains(dependencyName);
            if (!isCircularDependency && !compilationStack.isEmpty()) {
                DependencyReference dr = new DependencyReference(getCompilationStack().getFirst(), dependencyName);
                this.addDependencyReference(dr);
            }

            if (isCircularDependency) {
                throw new OpenLCompilationException(String.format("Circular dependency is detected: %s.",
                    buildCircularDependencyDetails(dependencyName, compilationStack)));
            }

            CompiledDependency compiledDependency;
            try {
                compilationStack.push(dependencyName);
                log.debug("Dependency '{}' is added to the compilation stack.", dependencyName);
                compiledDependency = dependencyLoader.getCompiledDependency();
            } finally {
                compilationStack.poll();
                log.debug("Dependency '{}' is removed from the compilation stack.", dependencyName);
            }

            if (compiledDependency == null) {
                if (dependencyLoader.isProject()) {
                    return throwCompilationError(dependency, dependencyLoader.getProject().getName());
                } else {
                    return throwCompilationError(dependency, dependencyName);
                }
            }
            return compiledDependency;
        } finally {
            if (compilationStack.isEmpty()) {
                compilationStackThreadLocal.remove(); // Clean thread
            }
        }
    }

    protected IDependencyLoader findDependencyLoader(IDependency dependency) throws OpenLCompilationException {
        final String dependencyName = dependency.getNode().getIdentifier();
        Collection<IDependencyLoader> loaders = getDependencyLoaders().get(dependencyName);
        if (loaders == null || loaders.isEmpty()) {
            throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", dependencyName),
                null,
                dependency.getNode().getSourceLocation());
        }
        if (loaders.size() > 1) {
            throw new OpenLCompilationException(
                String.format("Multiple modules with the same name '%s' are found.", dependencyName));
        }
        return loaders.iterator().next();
    }

    @Override
    public void clearOddDataForExecutionMode() {
        if (isExecutionMode() && getCompilationStack().isEmpty()) {
            for (Collection<IDependencyLoader> depLoaders : getDependencyLoaders().values()) {
                for (IDependencyLoader depLoader : depLoaders) {
                    CompiledDependency compiledDependency = depLoader.getRefToCompiledDependency();
                    if (compiledDependency != null) {
                        IOpenClass openClass = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors();
                        if (openClass instanceof ComponentOpenClass) {
                            ((ComponentOpenClass) openClass).clearOddDataForExecutionMode();
                        }
                    }
                }
            }
        }
    }

    private static String buildCircularDependencyDetails(String dependencyName, Deque<String> compilationStack) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = compilationStack.iterator();
        sb.append("'").append(dependencyName).append("'");
        while (itr.hasNext()) {
            String s = itr.next();
            sb.insert(0, "' -> ");
            sb.insert(0, s);
            sb.insert(0, "'");
            if (Objects.equals(dependencyName, s)) {
                break;
            }
        }
        return sb.toString();
    }

    private CompiledDependency throwCompilationError(IDependency dependency,
            String dependencyName) throws OpenLCompilationException {
        IdentifierNode node = dependency.getNode();
        throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", dependencyName),
            null,
            node.getSourceLocation(),
            node.getModule());
    }

    public synchronized ClassLoader getExternalJarsClassLoader(ProjectDescriptor project) {
        getDependencyLoaders(); // Init dependency loaders
        if (externalJarsClassloaders.get(project.getName()) != null) {
            return externalJarsClassloaders.get(project.getName());
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        OpenLBundleClassLoader externalJarsClassloader = new OpenLBundleClassLoader(project.getClassPathUrls(),
            parentClassLoader);
        // To load classes from dependency jars first
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
                    if (projectDependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        externalJarsClassloader.addClassLoader(getExternalJarsClassLoader(projectDescriptor));
                        break;
                    }
                }
            }
        }

        externalJarsClassloaders.put(project.getName(), externalJarsClassloader);
        return externalJarsClassloader;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return getDependencyLoaders().values()
            .stream()
            .flatMap(Collection::stream)
            .filter(IDependencyLoader::isProject)
            .map(IDependencyLoader::getProject)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    protected final void reset(IDependency dependency, Set<String> doNotDoTheSameResetTwice) {
        final String dependencyName = dependency.getNode().getIdentifier();
        if (doNotDoTheSameResetTwice.contains(dependencyName)) {
            return;
        }
        doNotDoTheSameResetTwice.add(dependencyName);

        List<DependencyReference> dependenciesToReset = new ArrayList<>();
        List<DependencyReference> dependenciesReferencesToClear = new ArrayList<>();
        for (DependencyReference dependencyReference : dependencyReferences) {
            if (dependencyReference.getReference().equals(dependencyName)) {
                dependenciesToReset.add(dependencyReference);
            }
            if (dependencyReference.getDependency().equals(dependencyName)) {
                dependenciesReferencesToClear.add(dependencyReference);
            }
        }

        for (DependencyReference dependencyReference : dependenciesToReset) {
            reset(new Dependency(DependencyType.MODULE,
                new IdentifierNode(dependency.getNode().getType(), null, dependencyReference.getDependency(), null)),
                doNotDoTheSameResetTwice);
        }

        Collection<IDependencyLoader> loaders = getDependencyLoaders().get(dependencyName);
        if (loaders != null) {
            loaders.forEach(IDependencyLoader::reset);
            for (DependencyReference dependencyReference : dependenciesReferencesToClear) {
                dependencyReferences.remove(dependencyReference);
            }
        }
    }

    @Override
    public synchronized void reset(IDependency dependency) {
        reset(dependency, new HashSet<>());
    }

    @Override
    public synchronized void resetAll() {
        for (ClassLoader classLoader : externalJarsClassloaders.values()) {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        externalJarsClassloaders.clear();
        for (Collection<IDependencyLoader> loaders : getDependencyLoaders().values()) {
            loaders.forEach(IDependencyLoader::reset);
        }
    }

    protected synchronized void addDependencyReference(DependencyReference dr) {
        dependencyReferences.add(dr);
    }

    /**
     * In execution mode all meta info that is not used in rules running is being cleaned.
     */
    @Override
    public boolean isExecutionMode() {
        return executionMode;
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

}
