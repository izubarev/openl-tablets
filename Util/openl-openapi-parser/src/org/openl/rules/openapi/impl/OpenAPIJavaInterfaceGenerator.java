package org.openl.rules.openapi.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.openl.gen.AnnotationDescriptionBuilder;
import org.openl.gen.JavaInterfaceByteCodeBuilder;
import org.openl.gen.MethodDescription;
import org.openl.gen.MethodDescriptionBuilder;
import org.openl.gen.MethodParameterBuilder;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.model.scaffolding.GeneratedJavaInterface;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.ruleservice.core.interceptors.RulesType;

/**
 * This is not junit test, it's only for the testing the results of the generation. This class functionality must be
 * used int the OpenAPIProjectCreator for the generation and RepositoryTreeController in case of the regeneration.
 */
public class OpenAPIJavaInterfaceGenerator {

    private static final Class<?> DEFAULT_DATATYPE_CLASS = Object.class;
    private static final Class<?> RULES_CTX_CLASS = IRulesRuntimeContext.class;

    private final ProjectModel projectModel;

    public OpenAPIJavaInterfaceGenerator(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public GeneratedJavaInterface generate() {
        JavaInterfaceByteCodeBuilder javaInterfaceBuilder = JavaInterfaceByteCodeBuilder
                .createWithDefaultPackage("OpenAPIService");
        boolean hasMethods = false;
        for (SpreadsheetModel method : projectModel.getSpreadsheetResultModels()) {
            if (method.getPathInfo().getOriginalPath().equals("/" + method.getPathInfo().getFormattedPath())) {
                continue;
            }
            MethodDescription methodDesc = visitInterfaceMethod(method, projectModel.isRuntimeContextProvided());
            if (methodDesc != null) {
                javaInterfaceBuilder.addAbstractMethod(methodDesc);
                hasMethods = true;
            }
        }
        if (hasMethods) {
            return new GeneratedJavaInterface(javaInterfaceBuilder.getNameWithPackage(),
                    javaInterfaceBuilder.build().byteCode());
        } else {
            return GeneratedJavaInterface.EMPTY;
        }
    }

    private MethodDescription visitInterfaceMethod(SpreadsheetModel sprModel, boolean runtimeContext) {
        final PathInfo pathInfo = sprModel.getPathInfo();
        final TypeInfo returnTypeInfo = pathInfo.getReturnType();
        MethodDescriptionBuilder methodBuilder = MethodDescriptionBuilder.create(pathInfo.getFormattedPath(),
                resolveType(returnTypeInfo));

        if (runtimeContext) {
            methodBuilder.addParameter(MethodParameterBuilder.create(RULES_CTX_CLASS.getName()).build());
        }

        for (InputParameter parameter : sprModel.getParameters()) {
            final TypeInfo paramType = parameter.getType();
            MethodParameterBuilder methodParamBuilder = MethodParameterBuilder.create(resolveType(paramType));
            if (paramType.isDatatype()) {
                methodParamBuilder.addAnnotation(AnnotationDescriptionBuilder.create(RulesType.class)
                        .withProperty("value", removeArray(paramType.getSimpleName()))
                        .build());
            }
            methodBuilder.addParameter(methodParamBuilder.build());
        }

        if (returnTypeInfo.isDatatype()) {
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(RulesType.class)
                    .withProperty("value", removeArray(returnTypeInfo.getSimpleName()))
                    .build());
        }

        methodBuilder.addAnnotation(AnnotationDescriptionBuilder
                .create(chooseOperationAnnotation(pathInfo.getOperation()))
                .build());
        methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Path.class)
                .withProperty("value", pathInfo.getOriginalPath())
                .build());
        methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Consumes.class)
                .withProperty("value", pathInfo.getConsumes(), true)
                .build());
        methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Produces.class)
                .withProperty("value", pathInfo.getProduces(), true)
                .build());

        return methodBuilder.build();
    }

    static String resolveType(TypeInfo typeInfo) {
        if (typeInfo.isDatatype()) {
            Class<?> type = DEFAULT_DATATYPE_CLASS;
            if (typeInfo.getDimension() > 0) {
                int[] dimensions = new int[typeInfo.getDimension()];
                type = Array.newInstance(type, dimensions).getClass();
            }
            return type.getName();
        } else {
            return typeInfo.getJavaName();
        }
    }

    private static String removeArray(String type) {
        int idx = type.indexOf('[');
        if (idx > 0) {
            return type.substring(0, idx);
        }
        return type;
    }

    private Class<? extends Annotation> chooseOperationAnnotation(String operation) {
        if (HttpMethod.GET.equalsIgnoreCase(operation)) {
            return GET.class;
        } else if (HttpMethod.POST.equalsIgnoreCase(operation)) {
            return POST.class;
        } else if (HttpMethod.PUT.equalsIgnoreCase(operation)) {
            return PUT.class;
        } else if (HttpMethod.DELETE.equalsIgnoreCase(operation)) {
            return DELETE.class;
        } else if (HttpMethod.PATCH.equalsIgnoreCase(operation)) {
            return PATCH.class;
        } else if (HttpMethod.HEAD.equals(operation)) {
            return HEAD.class;
        } else if (HttpMethod.OPTIONS.equals(operation)) {
            return OPTIONS.class;
        }
        throw new IllegalStateException("Unable to find operation annotation.");
    }
}
