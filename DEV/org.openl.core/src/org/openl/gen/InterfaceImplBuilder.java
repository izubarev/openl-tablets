package org.openl.gen;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.gen.groovy.GroovyInterfaceImplGenerator;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author Vladyslav Pikus
 */
public class InterfaceImplBuilder {

    private static final String DEFAULT_PACKAGE = "org.openl.generated";
    private static final String NAME_PATTERN = ".$%o%sImpl";
    private static final String SCRIPT_NAME_PATTERN = ".%s%oImpl";
    private static final AtomicInteger counter = new AtomicInteger();

    private final Class<?> clazzInterface;
    private final String beanName;
    private final String scriptName;
    private final Map<String, FieldDescription> beanFields = new TreeMap<>();
    private final List<MethodDescription> beanStubMethods = new ArrayList<>();

    public InterfaceImplBuilder(Class<?> clazzInterface, String packagePath) {
        if (!clazzInterface.isInterface()) {
            throw new IllegalArgumentException("Target class is not an interface.");
        }
        this.clazzInterface = clazzInterface;
        this.beanName = String
            .format(packagePath + NAME_PATTERN, counter.incrementAndGet(), clazzInterface.getSimpleName());
        this.scriptName = String
            .format(packagePath + SCRIPT_NAME_PATTERN, clazzInterface.getSimpleName(), counter.incrementAndGet());
        init();
    }

    public InterfaceImplBuilder(Class<?> clazzInterface) {
        this(clazzInterface, DEFAULT_PACKAGE);
    }

    private void init() {
        Set<Method> usedMethods = new HashSet<>();
        try {
            for (Class<?> it : clazzInterface.getInterfaces()) {
                collectFieldsAndMethods(it, usedMethods);
            }
            collectFieldsAndMethods(clazzInterface, usedMethods);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private void collectFieldsAndMethods(Class<?> clazzInterface,
            Set<Method> usedMethods) throws IntrospectionException {

        BeanInfo info = Introspector.getBeanInfo(clazzInterface);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();
        for (PropertyDescriptor property : properties) {
            usedMethods.add(property.getReadMethod());
            usedMethods.add(property.getWriteMethod());
            beanFields.put(property.getName(), new FieldDescription(property.getPropertyType().getName()));
        }
        MethodDescriptor[] methods = info.getMethodDescriptors();
        for (MethodDescriptor method : methods) {
            if (!usedMethods.contains(method.getMethod())) {
                Method methodRef = method.getMethod();
                MethodDescription methodDescription = new MethodDescription(method.getName(),
                    methodRef.getReturnType(),
                    methodRef.getParameterTypes());

                if (!beanStubMethods.contains(methodDescription)) {
                    beanStubMethods.add(methodDescription);
                }
            }
        }
    }

    public String getBeanName() {
        return beanName;
    }

    public String getScriptName() {
        return scriptName;
    }

    public byte[] byteCode() {
        return new JavaInterfaceImplGenerator(beanName, clazzInterface, beanFields, beanStubMethods).byteCode();
    }

    public String scriptText() {
        return new GroovyInterfaceImplGenerator(scriptName, clazzInterface, beanStubMethods).scriptText();
    }

}
