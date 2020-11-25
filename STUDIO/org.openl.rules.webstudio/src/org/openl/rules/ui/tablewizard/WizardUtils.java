package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.classes.ClassFinder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aliaksandr Antonik.
 */
public final class WizardUtils {

    private WizardUtils() {
    }

    private static final List<String> predefinedTypes;
    static {
        ArrayList<String> types = new ArrayList<>();

        // The most popular
        types.add("String");
        types.add("Double");
        types.add("Integer");
        types.add("Boolean");
        types.add("Date");

        types.add("BigInteger");
        types.add("BigDecimal");

        types.add("IntRange");
        types.add("DoubleRange");

        types.add("Long");
        types.add("Float");
        types.add("Short");
        types.add("Character");

        // Less popular
        types.add("byte");
        types.add("short");
        types.add("int");
        types.add("long");
        types.add("float");
        types.add("double");
        types.add("boolean");
        types.add("char");

        predefinedTypes = Collections.unmodifiableList(types);
    }
    static List<String> predefinedTypes() {
        return predefinedTypes;
    }

    static List<String> declaredDatatypes() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DatatypeOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> declaredAliases() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DomainOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> importedClasses() {
        return getImportedClasses().stream()
            .filter(t -> t instanceof JavaOpenClass)
            .map(v -> v.getDisplayName(INamedThing.SHORT))
            .sorted()
            .collect(Collectors.toList());
    }

    public static IOpenClass getProjectOpenClass() {
        return WebStudioUtils.getProjectModel().getCompiledOpenClass().getOpenClassWithErrors();
    }

    public static TableSyntaxNode[] getTableSyntaxNodes() {
        return WebStudioUtils.getProjectModel().getTableSyntaxNodes();
    }

    /**
     * Get imported classes for current project
     *
     * @return collection, containing an imported classes
     */
    public static Collection<IOpenClass> getImportedClasses() {
        Set<IOpenClass> classes = new TreeSet<>(
            (o1, o2) -> o1.getDisplayName(INamedThing.SHORT).compareToIgnoreCase(o2.getDisplayName(INamedThing.SHORT)));

        ClassFinder finder = new ClassFinder();
        for (String packageName : WebStudioUtils.getProjectModel().getXlsModuleNode().getImports()) {
            if ("org.openl.rules.enumeration".equals(packageName)) {
                // This package is added automatically in XlsLoader.addInnerImports() for inner usage, not for user.
                continue;
            }
            ClassLoader classLoader = WebStudioUtils.getProjectModel().getCompiledOpenClass().getClassLoader();
            for (Class<?> type : finder.getClasses(packageName, classLoader)) {
                IOpenClass openType;
                try {
                    openType = JavaOpenClass.getOpenClass(type);
                } catch (Exception e) {
                    // For example NoClassDefFoundError when the class for some of the fields is absent.
                    final Logger log = LoggerFactory.getLogger(WizardUtils.class);
                    log.debug("Cannot load the class, skip it because it's not valid. Cause: {}", e.getMessage(), e);
                    continue;
                }
                if (!isValid(openType)) {
                    continue;
                }

                classes.add(openType);
            }
        }

        return classes;
    }

    /**
     * Check if type is valid (for example, it can be used in a DataType tables, Data tables etc)
     *
     * @param openType checked type
     * @return true if class is valid.
     */
    private static boolean isValid(IOpenClass openType) {
        Class<?> instanceClass = openType.getInstanceClass();

        int modifiers = instanceClass.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return false;
        }

        // Every field has a "class" field. We skip a classes that does not
        // have any other field.
        return !openType.getFields().isEmpty();

    }
}
