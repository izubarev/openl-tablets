package org.openl.rules.ruleservice.publish.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.variation.VariationsPack;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.generation.GenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MethodUtils {
    private MethodUtils() {
    }


    private static void validateAndUpdateParameterNames(String[] parameterNames) {
        Set<String> allNames = new HashSet<>(Arrays.asList(parameterNames));
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < parameterNames.length; i++) {
            if (allNames.contains(parameterNames[i])) {
                allNames.remove(parameterNames[i]);
                usedNames.add(parameterNames[i]);
            } else {
                int j = 0;
                while (allNames.contains("arg" + j) || usedNames.contains("arg" + j)) {
                    j++;
                }
                parameterNames[i] = "arg" + j;
            }
        }
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass, Method method) {
        if (openClass != null) {
            return OpenClassHelper.findRulesMethod(openClass, method);
        }
        return null;
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass,
            Method method,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (openClass != null) {
            Class<?>[] parameterTypes = cutParameterTypes(method.getParameterTypes(),
                provideRuntimeContext,
                provideVariations);
            return OpenClassHelper.findRulesMethod(openClass, method.getName(), parameterTypes);
        }
        return null;
    }

    private static Class<?>[] cutParameterTypes(Class<?>[] parameterTypes,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (provideRuntimeContext) {
            parameterTypes = Arrays.copyOfRange(parameterTypes, 1, parameterTypes.length);
        }
        if (provideVariations) {
            if (parameterTypes.length > 0 && Objects.equals(parameterTypes[parameterTypes.length - 1],
                VariationsPack.class))
                parameterTypes = Arrays.copyOfRange(parameterTypes, 0, parameterTypes.length - 1);
        }
        return parameterTypes;
    }

    public static IOpenMember findRulesMember(IOpenClass openClass,
            Method method,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Class<?>[] parameterTypes = cutParameterTypes(method.getParameterTypes(),
            provideRuntimeContext,
            provideVariations);
        return OpenClassHelper.findRulesMember(openClass, method.getName(), parameterTypes);
    }

    public static String[] getParameterNames(IOpenClass openClass,
            Method method,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (openClass != null) {
            String[] parameterNames = GenUtils
                .getParameterNames(method, openClass, provideRuntimeContext, provideVariations);
            int i = 0;
            for (Annotation[] annotations : method.getParameterAnnotations()) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Name) {
                        Name name = (Name) annotation;
                        if (!name.value().isEmpty()) {
                            parameterNames[i] = name.value();
                        } else {
                            Logger log = LoggerFactory.getLogger(MethodUtils.class);
                            if (log.isWarnEnabled()) {
                                log.warn(
                                    "Invalid parameter name '{}' is used in @Name annotation for the method '{}.{}'.",
                                    name.value(),
                                    method.getClass().getTypeName(),
                                    MethodUtil.printMethod(method.getName(), method.getParameterTypes()));
                            }
                        }
                    }
                }
                i++;
            }
            validateAndUpdateParameterNames(parameterNames);
            return parameterNames;
        }
        return GenUtils.getParameterNames(method);
    }
}
