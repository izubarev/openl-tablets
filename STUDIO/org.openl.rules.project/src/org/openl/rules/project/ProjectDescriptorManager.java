package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ProjectDescriptorValidator;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.rits.cloning.Cloner;

public class ProjectDescriptorManager {

    private IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private final ProjectDescriptorValidator validator = new ProjectDescriptorValidator();
    private PathMatcher pathMatcher = new AntPathMatcher();

    private final Cloner cloner = new SafeCloner();

    public PathMatcher getPathMatcher() {
        return pathMatcher;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public IProjectDescriptorSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(IProjectDescriptorSerializer serializer) {
        this.serializer = serializer;
    }

    private ProjectDescriptor readDescriptorInternal(InputStream source) {
        return serializer.deserialize(source);
    }

    public ProjectDescriptor readDescriptor(File file) throws IOException, ValidationException {
        FileInputStream inputStream = new FileInputStream(file);

        ProjectDescriptor descriptor = readDescriptorInternal(inputStream);
        IOUtils.closeQuietly(inputStream);

        postProcess(descriptor, file);
        validator.validate(descriptor);

        return descriptor;
    }

    public ProjectDescriptor readDescriptor(String filename) throws IOException, ValidationException {
        File source = new File(filename);
        return readDescriptor(source);
    }

    public ProjectDescriptor readOriginalDescriptor(File filename) throws FileNotFoundException, ValidationException {
        FileInputStream inputStream = new FileInputStream(filename);

        ProjectDescriptor descriptor = readDescriptorInternal(inputStream);
        IOUtils.closeQuietly(inputStream);

        validator.validate(descriptor);

        return descriptor;
    }

    public void writeDescriptor(ProjectDescriptor descriptor, OutputStream dest) throws IOException,
                                                                                 ValidationException {
        validator.validate(descriptor);
        descriptor = cloner.deepClone(descriptor); // prevent changes argument
        // object
        preProcess(descriptor);
        String serializedObject = serializer.serialize(descriptor);
        dest.write(serializedObject.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isModuleWithWildcard(Module module) {
        PathEntry rulesRootPath = module.getRulesRootPath();
        if (rulesRootPath != null) {
            String path = rulesRootPath.getPath();
            return path.contains("*") || path.contains("?");
        }
        return false;
    }

    public boolean isCoveredByWildcardModule(ProjectDescriptor descriptor, Module otherModule) {
        for (Module module : descriptor.getModules()) {
            final PathEntry otherModuleRootPath = otherModule.getRulesRootPath();
            if (isModuleWithWildcard(module) && otherModuleRootPath != null) {
                String relativePath = otherModuleRootPath.getPath().replace("\\", "/");
                if (pathMatcher.match(module.getRulesRootPath().getPath(), relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Module> getAllModulesMatchingPathPattern(ProjectDescriptor descriptor,
            Module module,
            String pathPattern) throws IOException {
        List<Module> modules = new ArrayList<>();

        String ptrn = pathPattern.trim();
        Path rootPath = descriptor.getProjectFolder().toPath();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = rootPath.relativize(file).toString().replace("\\", "/");
                if (isNotTemporaryFile(file) && pathMatcher.match(ptrn, relativePath)) {
                    String moduleFile = file.toAbsolutePath().toString();
                    Module m = new Module();
                    m.setProject(descriptor);
                    m.setRulesRootPath(new PathEntry(moduleFile));
                    m.setName(FileUtils.getBaseName(moduleFile));
                    m.setMethodFilter(module.getMethodFilter());
                    m.setWildcardRulesRootPath(pathPattern);
                    m.setWildcardName(module.getName());
                    modules.add(m);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return modules;
    }

    private boolean isNotTemporaryFile(Path file) throws IOException {
        if (file.getFileName().startsWith("~$") || Files.isHidden(file)) {
            OutputStream os = null;
            try {
                os = Files.newOutputStream(file, StandardOpenOption.APPEND);
            } catch (Exception unused) {
                return false;
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
        return true;
    }

    private boolean containsInProcessedModules(Collection<Module> modules, Module m, File projectRoot) {
        PathEntry pathEntry = m.getRulesRootPath();
        if (!new File(m.getRulesRootPath().getPath()).isAbsolute()) {
            pathEntry = new PathEntry(new File(projectRoot, m.getRulesRootPath().getPath()).getAbsolutePath());
        }

        for (Module module : modules) {
            PathEntry modulePathEntry = module.getRulesRootPath();
            if (!new File(module.getRulesRootPath().getPath()).isAbsolute()) {
                modulePathEntry = new PathEntry(
                    new File(projectRoot, module.getRulesRootPath().getPath()).getAbsolutePath());
            }
            if (pathEntry.getPath().equals(modulePathEntry.getPath())) {
                return true;
            }
        }
        return false;
    }

    private void processModulePathPatterns(ProjectDescriptor descriptor, File projectRoot) throws IOException {
        List<Module> modulesWasRead = descriptor.getModules();
        List<Module> processedModules = new ArrayList<>(modulesWasRead.size());
        // Process modules without wildcard path
        for (Module module : modulesWasRead) {
            if (!isModuleWithWildcard(module)) {
                processedModules.add(module);
            }
        }
        // Process modules with wildcard path
        for (Module module : modulesWasRead) {
            if (isModuleWithWildcard(module)) {
                List<Module> newModules = new ArrayList<>();
                List<Module> modules = getAllModulesMatchingPathPattern(descriptor,
                    module,
                    module.getRulesRootPath().getPath());
                for (Module m : modules) {
                    if (!containsInProcessedModules(processedModules, m, projectRoot)) {
                        newModules.add(m);
                    }
                }
                processedModules.addAll(newModules);
            }
        }

        descriptor.setModules(processedModules);
    }

    private void postProcess(ProjectDescriptor descriptor, File projectDescriptorFile) throws IOException {
        File projectRoot = projectDescriptorFile.getParentFile().getCanonicalFile();
        descriptor.setProjectFolder(projectRoot);
        processModulePathPatterns(descriptor, projectRoot);

        for (Module module : descriptor.getModules()) {
            module.setProject(descriptor);
            if (module.getMethodFilter() == null) {
                module.setMethodFilter(new MethodFilter());
            }
            if (module.getMethodFilter().getExcludes() == null) {
                module.getMethodFilter().setExcludes(new HashSet<String>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getExcludes().removeAll(Arrays.asList("", null));
            }

            if (module.getMethodFilter().getIncludes() == null) {
                module.getMethodFilter().setIncludes(new HashSet<String>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getIncludes().removeAll(Arrays.asList("", null));
            }

            if (!new File(module.getRulesRootPath().getPath()).isAbsolute()) {
                PathEntry absolutePath = new PathEntry(
                    new File(projectRoot, module.getRulesRootPath().getPath()).getCanonicalFile().getAbsolutePath());
                module.setRulesRootPath(absolutePath);
            }
        }
    }

    private void preProcess(ProjectDescriptor descriptor) {
        // processModulePathPatterns(descriptor);
        if (descriptor.getModules() == null || descriptor.getModules().isEmpty()) {
            return;
        }
        Set<String> wildcardPathSet = new HashSet<>();
        Iterator<Module> itr = descriptor.getModules().iterator();
        while (itr.hasNext()) {
            Module module = itr.next();
            if (module.getWildcardRulesRootPath() == null || !wildcardPathSet
                .contains(module.getWildcardRulesRootPath())) {
                module.setProject(null);
                module.setProperties(null);
                if (module.getWildcardRulesRootPath() != null) {
                    wildcardPathSet.add(module.getWildcardRulesRootPath());
                    module.setRulesRootPath(new PathEntry(module.getWildcardRulesRootPath()));
                    module.setName(module.getWildcardName());
                } else {
                    PathEntry pathEntry = module.getRulesRootPath();
                    String path = pathEntry.getPath();
                    module.setRulesRootPath(new PathEntry(path.replaceAll("\\\\", "/")));
                }
                if (module.getMethodFilter() != null) {
                    boolean f = true;
                    if (module.getMethodFilter().getExcludes() != null && module.getMethodFilter()
                        .getExcludes()
                        .isEmpty()) {
                        module.getMethodFilter().setExcludes(null);
                        f = false;
                    }
                    if (module.getMethodFilter().getIncludes() != null && module.getMethodFilter()
                        .getIncludes()
                        .isEmpty()) {
                        if (f) {
                            module.getMethodFilter().setExcludes(null);
                        } else {
                            module.setMethodFilter(null);
                        }
                    }
                }
            } else {
                itr.remove();
            }
        }
    }

}
