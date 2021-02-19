package org.openl.rules.ruleservice.core;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.ModuleSpecificType;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.NotConvertor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.TypeResolver;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;
import org.openl.rules.ruleservice.core.interceptors.converters.SPRToPlainConverterAdvice;
import org.openl.rules.ruleservice.core.interceptors.converters.VariationResultSPRToPlainConverterAdvice;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.variation.VariationsResult;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuleServiceInstantiationFactoryHelper {
    private RuleServiceInstantiationFactoryHelper() {
        // Hidden constructor
    }

    /**
     * Special ClassVisitor to generate interface with {@link Object} as the return type for methods that have "after
     * interceptors".
     *
     * @author PUdalau
     */
    private static class RuleServiceInterceptorsSupportClassVisitor extends ClassVisitor {
        private final Map<String, List<Pair<Method, Pair<Class<?>, Boolean>>>> methodsWithReturnTypeNeedsChange;
        private final Map<String, List<Method>> methodsToRemove;

        /**
         * Constructs instance with delegated {@link ClassVisitor} and set of methods.
         *
         * @param visitor delegated {@link ClassVisitor}.
         * @param methodsWithReturnTypeNeedsChange Methods where to change return type.
         */
        private RuleServiceInterceptorsSupportClassVisitor(ClassVisitor visitor,
                Map<Method, Pair<Class<?>, Boolean>> methodsWithReturnTypeNeedsChange,
                Collection<Method> methodsToRemove) {
            super(Opcodes.ASM5, visitor);
            Objects.requireNonNull(methodsWithReturnTypeNeedsChange, "methodsWithReturnTypeNeedsChange cannot be null");
            this.methodsWithReturnTypeNeedsChange = new HashMap<>();
            // Build map by method name to improve performance of the method search loop
            for (Entry<Method, Pair<Class<?>, Boolean>> entry : methodsWithReturnTypeNeedsChange.entrySet()) {
                List<Pair<Method, Pair<Class<?>, Boolean>>> listOfMethods = this.methodsWithReturnTypeNeedsChange
                    .computeIfAbsent(entry.getKey().getName(), e -> new ArrayList<>());
                listOfMethods.add(Pair.of(entry.getKey(), entry.getValue()));
            }
            Objects.requireNonNull(methodsToRemove, "methodsToRemove cannot be null");
            this.methodsToRemove = new HashMap<>();
            // Build map by method name to improve performance of the method search loop
            for (Method method : methodsToRemove) {
                List<Method> listOfMethods = this.methodsToRemove.computeIfAbsent(method.getName(),
                    e -> new ArrayList<>());
                listOfMethods.add(method);
            }

        }

        @Override
        public MethodVisitor visitMethod(final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            List<Method> listOfMethodsToRemove = methodsToRemove.get(name);
            if (listOfMethodsToRemove != null) {
                for (Method method : listOfMethodsToRemove) {
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        return null;
                    }
                }
            }
            List<Pair<Method, Pair<Class<?>, Boolean>>> listOfMethods = methodsWithReturnTypeNeedsChange.get(name);
            if (listOfMethods != null) {
                for (Pair<Method, Pair<Class<?>, Boolean>> entry : listOfMethods) {
                    Method method = entry.getKey();
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        Class<?> newRetType = entry.getValue().getKey();
                        MethodVisitor mv = super.visitMethod(access,
                            name,
                            Type.getMethodDescriptor(Type.getType(newRetType), Type.getArgumentTypes(descriptor)),
                            signature,
                            exceptions);
                        if (!entry.getValue().getValue()) {
                            AnnotationVisitor av = mv
                                .visitAnnotation(Type.getDescriptor(ServiceCallAfterInterceptor.class), true);
                            AnnotationVisitor av1 = av.visitArray("value");
                            av1.visit("value",
                                Type.getType(VariationsResult.class
                                    .equals(newRetType) ? VariationResultSPRToPlainConverterAdvice.class
                                                        : SPRToPlainConverterAdvice.class));
                            av1.visitEnd();
                            av.visitEnd();
                        }
                        return mv;
                    }
                }
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$Original";

    /**
     * Returns service class for instantiation strategy according to after interceptors of methods in service class of
     * service specified as the argument.
     *
     * @param serviceClass Interface for service, which will be used for service class creation.
     * @return Service class for instantiation strategy based on service class for service.
     */
    public static Class<?> buildInterfaceForInstantiationStrategy(Class<?> serviceClass,
            ClassLoader classLoader,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(null, serviceClass, true, false, classLoader, provideRuntimeContext, provideVariations);
    }

    public static Class<?> buildInterfaceForService(IOpenClass openClass,
            Class<?> serviceClass,
            ClassLoader classLoader,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(openClass,
            serviceClass,
            false,
            true,
            classLoader,
            provideRuntimeContext,
            provideVariations);
    }

    public static Class<?> processInterface(IOpenClass openClass,
            Class<?> serviceClass,
            boolean removeServiceExtraMethods,
            boolean toServiceClass,
            ClassLoader classLoader,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");

        Map<Method, Pair<Class<?>, Boolean>> methodsWithReturnTypeNeedsChange = getMethodsWithReturnTypeNeedsChange(
            openClass,
            serviceClass,
            toServiceClass,
            provideRuntimeContext,
            provideVariations);

        Set<Method> methodsToRemove = getMethodsToRemove(serviceClass, removeServiceExtraMethods);

        if (methodsWithReturnTypeNeedsChange.isEmpty() && methodsToRemove.isEmpty()) {
            return serviceClass;
        } else {
            Logger log = LoggerFactory.getLogger(RuleServiceInstantiationFactoryHelper.class);
            for (Entry<Method, Pair<Class<?>, Boolean>> entry : methodsWithReturnTypeNeedsChange.entrySet()) {
                Method method = entry.getKey();
                if (method.getReturnType().isAssignableFrom(entry.getValue().getKey()) && log.isWarnEnabled()) {
                    log.warn(
                        "Wrong return type for method '{}' in class '{}' is used. Return type is replaced to '{}'.",
                        MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                        serviceClass.getName(),
                        entry.getValue().getKey().getTypeName());
                }
            }

            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new RuleServiceInterceptorsSupportClassVisitor(classWriter,
                methodsWithReturnTypeNeedsChange,
                methodsToRemove);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, className);
            transformer.accept(classVisitor);
            classWriter.visitEnd();
            try {
                // Create class object.
                //
                return ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);
            } catch (Exception e) {
                throw new OpenlNotCheckedException(e);
            }
        }
    }

    private static Class<?> getGenericType(Class<?> clazz) {
        if (clazz
            .getGenericSuperclass() instanceof ParameterizedType && ((ParameterizedType) clazz.getGenericSuperclass())
                .getActualTypeArguments()[0] instanceof Class) {
            return (Class<?>) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return Object.class;
    }

    private static Class<? extends ServiceMethodAfterAdvice<?>> getLastServiceMethodAfterAdvice(
            ServiceCallAfterInterceptor serviceCallAfterInterceptor) {
        Class<? extends ServiceMethodAfterAdvice<?>>[] interceptors = serviceCallAfterInterceptor.value();
        int i = interceptors.length - 1;
        while (i >= 0) {
            Class<? extends ServiceMethodAfterAdvice<?>> serviceMethodAfterAdvice = interceptors[i];
            if (!serviceMethodAfterAdvice.isAnnotationPresent(NotConvertor.class)) {
                return serviceMethodAfterAdvice;
            }
            i--;
        }
        return null;
    }

    private static Class<?> notNullIfNewMethodReturnTypeWithAnnotations(IOpenClass openClass,
            Method method,
            boolean toServiceClass,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        ServiceCallAfterInterceptor serviceCallAfterInterceptor = method
            .getAnnotation(ServiceCallAfterInterceptor.class);
        if (serviceCallAfterInterceptor != null && (!provideVariations || !method.getReturnType()
            .equals(VariationsResult.class))) {
            Class<? extends ServiceMethodAfterAdvice<?>> lastServiceMethodAfterAdvice = getLastServiceMethodAfterAdvice(
                serviceCallAfterInterceptor);
            if (lastServiceMethodAfterAdvice != null) {
                return extractTypeForMethod(openClass,
                    method,
                    toServiceClass,
                    lastServiceMethodAfterAdvice,
                    provideRuntimeContext,
                    provideVariations);
            }
        }

        ServiceCallAroundInterceptor serviceCallAroundInterceptor = method
            .getAnnotation(ServiceCallAroundInterceptor.class);
        if (serviceCallAroundInterceptor != null && (!provideVariations || !method.getReturnType()
            .equals(VariationsResult.class))) {
            Class<? extends ServiceMethodAroundAdvice<?>> serviceMethodAroundAdvice = serviceCallAroundInterceptor
                .value();
            return extractTypeForMethod(openClass,
                method,
                toServiceClass,
                serviceMethodAroundAdvice,
                provideRuntimeContext,
                provideVariations);
        }
        return null;
    }

    private static Class<?> extractTypeForMethod(IOpenClass openClass,
            Method method,
            boolean toServiceClass,
            Class<? extends ServiceMethodAdvice> serviceMethodAdvice,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (toServiceClass) {
            UseOpenMethodReturnType useOpenMethodReturnType = serviceMethodAdvice
                .getAnnotation(UseOpenMethodReturnType.class);
            if (useOpenMethodReturnType != null) {
                Class<?> t = extractOpenMethodReturnType(openClass,
                    method,
                    serviceMethodAdvice,
                    useOpenMethodReturnType.value(),
                    provideRuntimeContext,
                    provideVariations);
                if (t != null) {
                    return t;
                }
            }
            if (serviceMethodAdvice.isAnnotationPresent(NotConvertor.class)) {
                return null;
            }
            return getGenericType(serviceMethodAdvice);
        }
        return Object.class;
    }

    private static Class<?> extractOpenMethodReturnType(IOpenClass openClass,
            Method method,
            Class<?> serviceClass,
            TypeResolver typeResolver,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        IOpenMember openMember = MethodUtils
            .findRulesMember(openClass, method, provideRuntimeContext, provideVariations);
        if (openMember == null) {
            logWarn(method, serviceClass);
            return null;
        }
        IOpenClass returnType = openMember.getType();
        switch (typeResolver) {
            case ORIGINAL:
                return returnType.getInstanceClass();
            case IF_SPR_TO_PLAIN:
                IOpenClass type = returnType;
                int dim = 0;
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dim++;
                }
                XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                if (type instanceof CustomSpreadsheetResultOpenClass) {
                    Class<?> t = ((CustomSpreadsheetResultOpenClass) module.findType(type.getName())).getBeanClass();
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                } else if (type instanceof SpreadsheetResultOpenClass) {
                    Class<?> t;
                    // Check: custom spreadsheet is enabled
                    if (module.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                        t = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                            .toCustomSpreadsheetResultOpenClass()
                            .getBeanClass();
                    } else {
                        t = type.getInstanceClass();
                    }
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;

                } else {
                    if (type instanceof ModuleSpecificType) {
                        Class<?> t = module.findType(type.getName()).getInstanceClass();
                        return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                    } else {
                        return returnType.getInstanceClass();
                    }
                }
            default:
                throw new IllegalStateException();
        }
    }

    private static void logWarn(Method method, Class<?> interceptorClass) {
        Logger log = LoggerFactory.getLogger(RuleServiceInstantiationFactoryHelper.class);

        if (log.isWarnEnabled()) {
            log.warn(
                "Method return type is not found for '{}.{}'. Please, make sure that @OpenMethodReturnType is used correctly in '{}' interceptor class.",
                method.getClass().getTypeName(),
                MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                interceptorClass.getTypeName());
        }
    }

    private static boolean isMethodWithServiceExtraMethodAnnotation(Method method) {
        return method.getAnnotation(ServiceExtraMethod.class) != null;
    }

    private static boolean isTypeChangingAnnotationPresent(Method method) {
        return method.isAnnotationPresent(ServiceCallAfterInterceptor.class) || method
            .isAnnotationPresent(ServiceCallAroundInterceptor.class);
    }

    /**
     * Look through all methods of the specified class in order to find all methods annotated by
     * {@link ServiceCallAfterInterceptor}.
     *
     * @param serviceClass Class to be analyzed.
     * @return Methods which have after interceptors.
     */
    private static Map<Method, Pair<Class<?>, Boolean>> getMethodsWithReturnTypeNeedsChange(IOpenClass openClass,
            Class<?> serviceClass,
            boolean toServiceClass,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Map<Method, Pair<Class<?>, Boolean>> ret = new HashMap<>();
        for (Method method : serviceClass.getMethods()) {
            Class<?> newReturnType = notNullIfNewMethodReturnTypeWithAnnotations(openClass,
                method,
                toServiceClass,
                provideRuntimeContext,
                provideVariations);
            if (newReturnType != null) {
                ret.put(method, Pair.of(newReturnType, Boolean.TRUE));
            } else if (toServiceClass && !isTypeChangingAnnotationPresent(method) && !method
                .isAnnotationPresent(ServiceExtraMethod.class)) {
                IOpenMember openMember = MethodUtils
                    .findRulesMember(openClass, method, provideRuntimeContext, provideVariations);
                if (openMember == null) {
                    throw new IllegalStateException("Open member is not found.");
                }
                IOpenClass type = openMember.getType();
                int dim = 0;
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dim++;
                }
                if (provideVariations && method.getReturnType().equals(VariationsResult.class)) {
                    ret.put(method, Pair.of(VariationsResult.class, Boolean.FALSE));
                } else if (type instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) type;
                    XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                    CustomSpreadsheetResultOpenClass csrt = (CustomSpreadsheetResultOpenClass) module
                        .findType(customSpreadsheetResultOpenClass.getName());
                    Class<?> t = csrt.getBeanClass();
                    if (dim > 0) {
                        t = Array.newInstance(t, new int[dim]).getClass();
                    }
                    ret.put(method, Pair.of(t, Boolean.FALSE));
                } else if (type instanceof SpreadsheetResultOpenClass) {
                    XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                    // Check: custom spreadsheet is enabled
                    if (module.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                        Class<?> t = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                            .toCustomSpreadsheetResultOpenClass()
                            .getBeanClass();
                        if (dim > 0) {
                            t = Array.newInstance(t, new int[dim]).getClass();
                        }
                        ret.put(method, Pair.of(t, Boolean.FALSE));
                    }
                } else if (JavaOpenClass.OBJECT.equals(type)) {
                    ret.put(method, Pair.of(openMember.getType().getInstanceClass(), Boolean.FALSE));
                }
            }
        }
        return ret;
    }

    /**
     * Look through all methods of the specified class in order to find all methods which must be excluded from
     * interface
     *
     * @param serviceClass Class to be analyzed.
     * @param removeServiceExtraMethods {@code true} if methods annotated by {@link ServiceExtraMethod} must be excluded
     * @return Methods which have after interceptors.
     */
    private static Set<Method> getMethodsToRemove(Class<?> serviceClass, boolean removeServiceExtraMethods) {
        Set<Method> ret = new HashSet<>();
        for (Method method : serviceClass.getMethods()) {
            if (ITableProperties.class.isAssignableFrom(method
                .getReturnType()) || (removeServiceExtraMethods && isMethodWithServiceExtraMethodAnnotation(method))) {
                ret.add(method);
            }
        }
        return ret;
    }

}
