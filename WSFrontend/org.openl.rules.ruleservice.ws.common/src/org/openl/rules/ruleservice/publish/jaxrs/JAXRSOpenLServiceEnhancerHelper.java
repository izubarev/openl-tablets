package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.xml.ElementClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.ruleservice.publish.common.ExceptionResponseDto;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.ClassUtils;
import org.openl.util.JAXBUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.GenUtils;
import org.openl.util.generation.InterfaceTransformer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;

public class JAXRSOpenLServiceEnhancerHelper {
    private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXRSAnnotated";

    private static final Set<Class<?>> TEXT_MEDIA_TYPE_SET = new HashSet<>();
    static {
        TEXT_MEDIA_TYPE_SET.add(Number.class);
        TEXT_MEDIA_TYPE_SET.add(Enum.class);
        TEXT_MEDIA_TYPE_SET.add(String.class);
        TEXT_MEDIA_TYPE_SET.add(Date.class);
        TEXT_MEDIA_TYPE_SET.add(Instant.class);
        TEXT_MEDIA_TYPE_SET.add(ZonedDateTime.class);
        TEXT_MEDIA_TYPE_SET.add(LocalDateTime.class);
        TEXT_MEDIA_TYPE_SET.add(LocalDate.class);
        TEXT_MEDIA_TYPE_SET.add(LocalTime.class);
    }

    private static class JAXRSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DEFAULT_VERSION = "1.0.0";

        private static final String UNPROCESSABLE_ENTITY_MESSAGE = "Custom user errors in rules or validation errors in input parameters";
        private static final String UNPROCESSABLE_ENTITY_EXAMPLE = "{\"message\": \"Some message\", \"type\": \"USER_ERROR\"}";
        private static final String BAD_REQUEST_MESSAGE = "Invalid request format e.g. missing required field, unparseable JSON value, etc.";
        private static final String BAD_REQUEST_EXAMPLE = "{\"message\": \"Cannot parse 'bar' to JSON\", \"type\": \"BAD_REQUEST\"}";
        private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server errors e.g. compilation or parsing errors, runtime exceptions, etc.";
        private static final String INTERNAL_SERVER_ERROR_EXAMPLE = "{\"message\": \"Failed to load lazy method.\", \"type\": \"COMPILATION\"}";

        private static final int MAX_PARAMETERS_COUNT_FOR_GET = 4;

        private static final String REQUEST_PARAMETER_SUFFIX = "Request";

        private final Class<?> originalClass;
        private final ClassLoader classLoader;
        private Map<Method, String> paths = null;
        private Map<Method, String> nicknames = null;
        private Map<Method, String> methodRequests = null;
        private final IOpenClass openClass;
        private final String serviceName;
        private final String serviceExposedUrl;
        private final boolean resolveMethodParameterNames;
        private final boolean provideRuntimeContext;
        private final boolean provideVariations;

        JAXRSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> originalClass,
                IOpenClass openClass,
                ClassLoader classLoader,
                String serviceName,
                String serviceExposedUrl,
                boolean resolveMethodParameterNames,
                boolean provideRuntimeContext,
                boolean provideVariations) {
            super(Opcodes.ASM5, arg0);
            this.serviceName = serviceName;
            this.originalClass = originalClass;
            this.openClass = openClass;
            this.classLoader = classLoader;
            this.serviceExposedUrl = serviceExposedUrl;
            this.resolveMethodParameterNames = resolveMethodParameterNames;
            this.provideRuntimeContext = provideRuntimeContext;
            this.provideVariations = provideVariations;
        }

        @Override
        public void visit(int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            // Swagger annotation
            if (!originalClass.isAnnotationPresent(Api.class)) {
                this.visitAnnotation(Type.getDescriptor(Api.class), true);
            }
            if (!originalClass.isAnnotationPresent(SwaggerDefinition.class)) {
                AnnotationVisitor av = this.visitAnnotation(Type.getDescriptor(SwaggerDefinition.class), true);
                AnnotationVisitor av1 = av.visitAnnotation("info",
                    Type.getDescriptor(io.swagger.annotations.Info.class));
                av1.visit("title", serviceName);
                av1.visit("version", DEFAULT_VERSION);
                av1.visitEnd();
                av.visitEnd();
            }

            // OpenAPI annotation
            if (!originalClass.isAnnotationPresent(OpenAPIDefinition.class)) {
                AnnotationVisitor av = this.visitAnnotation(Type.getDescriptor(OpenAPIDefinition.class), true);
                AnnotationVisitor av1 = av.visitAnnotation("info", Type.getDescriptor(Info.class));
                av1.visit("title", serviceName);
                av1.visit("version", DEFAULT_VERSION);
                av1.visitEnd();

                if (serviceExposedUrl != null) {
                    AnnotationVisitor av2 = av.visitArray("servers");
                    AnnotationVisitor av3 = av2.visitAnnotation("servers", Type.getDescriptor(Server.class));
                    av3.visit("url", serviceExposedUrl);
                    av3.visitEnd();
                    av2.visitEnd();
                }

                av.visitEnd();
            }

            if (!originalClass.isAnnotationPresent(Path.class)) {
                AnnotationVisitor annotationVisitor = this.visitAnnotation(Type.getDescriptor(Path.class), true);
                annotationVisitor.visit("value", "/");
                annotationVisitor.visitEnd();
            }

            // Consumes annotation
            if (!originalClass.isAnnotationPresent(Consumes.class)) {
                addConsumesAnnotation(this);
            }
            // Produces annotation
            if (!originalClass.isAnnotationPresent(Produces.class)) {
                addProducesAnnotation(this);
            }
            // Error responses annotation
            if (!originalClass.isAnnotationPresent(io.swagger.annotations.ApiResponses.class) && !originalClass
                .isAnnotationPresent(io.swagger.annotations.ApiResponse.class)) {
                addSwaggerApiResponsesAnnotation(this);
            }
            if (!originalClass
                .isAnnotationPresent(io.swagger.v3.oas.annotations.responses.ApiResponses.class) && !originalClass
                    .isAnnotationPresent(io.swagger.v3.oas.annotations.responses.ApiResponse.class)) {
                addOpenApiResponsesAnnotation(this);
            }
        }

        private String changedParameterTypesDescription(String descriptor, Method originalMethod) throws Exception {
            Class<?> parameterWrapperClass = generateWrapperClass(originalMethod);
            return Type.getMethodDescriptor(Type.getReturnType(descriptor), Type.getType(parameterWrapperClass));
        }

        private Class<?> generateWrapperClass(Method originalMethod) throws Exception {
            String[] parameterNames = resolveParameterNames(originalMethod);
            String requestParameterName = getRequestParameterName(originalMethod);
            String beanName = "org.openl.jaxrs." + requestParameterName;

            int i = 0;
            WrapperBeanClassBuilder beanClassBuilder = new WrapperBeanClassBuilder(beanName, originalMethod.getName());
            for (Class<?> type : originalMethod.getParameterTypes()) {
                beanClassBuilder.addField(parameterNames[i], type.getName());
                i++;
            }

            byte[] byteCode = beanClassBuilder.byteCode();

            return ClassUtils.defineClass(beanName, byteCode, classLoader);
        }

        String getRequestParameterName(Method method) {
            if (methodRequests == null) {
                methodRequests = new HashMap<>();
                List<Method> methods = MethodUtils.sort(Arrays.asList(originalClass.getMethods()));

                Set<String> requestEntitiesCache = initRequestEntitiesCache(methods);
                for (Method m : methods) {
                    String name = StringUtils.capitalize(m.getName()) + REQUEST_PARAMETER_SUFFIX;
                    String s = name;
                    int i = 1;
                    while (requestEntitiesCache.contains(s)) {
                        s = name + i;
                        i++;
                    }
                    requestEntitiesCache.add(s);
                    methodRequests.put(m, s);
                }
            }

            return methodRequests.get(method);
        }

        private Set<String> initRequestEntitiesCache(List<Method> methods) {
            Set<String> cache = new HashSet<>();
            for (Method method : methods) {
                for (Class<?> paramType : method.getParameterTypes()) {
                    String requestEntityName = paramType.getSimpleName();
                    if (requestEntityName.contains(REQUEST_PARAMETER_SUFFIX)) {
                        cache.add(requestEntityName);
                    }
                }
            }
            return cache;
        }

        String getNickName(Method method) {
            if (nicknames == null) {
                nicknames = new HashMap<>();
                List<Method> methods = new ArrayList<>();
                List<Method> methods1 = new ArrayList<>();
                for (Method m : originalClass.getMethods()) {
                    if (m.isAnnotationPresent(Path.class)) {
                        methods1.add(m);
                    } else {
                        methods.add(m);
                    }
                }
                generateNickNames(methods);
                generateNickNames(methods1);
            }
            return nicknames.get(method);
        }

        private void generateNickNames(List<Method> methods) {
            methods = MethodUtils.sort(methods);
            for (Method m : methods) {
                String s = m.getName();
                int i = 1;
                while (nicknames.containsValue(s)) {
                    s = m.getName() + "_" + i;
                    i++;
                }
                nicknames.put(m, s);
            }
        }

        String getPath(Method method) {
            if (paths == null) {
                paths = new HashMap<>();
                List<Method> methods = new ArrayList<>();
                for (Method m : originalClass.getMethods()) {
                    Path pathAnnotation = m.getAnnotation(Path.class);
                    if (pathAnnotation != null) {
                        String value = pathAnnotation.value();

                        while (value.charAt(0) == '/') {
                            value = value.substring(1);
                        }

                        if (value.indexOf('/') > 0) {
                            value = value.substring(0, value.indexOf('/'));
                        }

                        paths.put(m, value);
                    } else {
                        methods.add(m);
                    }
                }

                methods = MethodUtils.sort(methods);

                for (Method m : methods) {
                    String s = m.getName();
                    int i = 1;
                    while (paths.containsValue(s)) {
                        s = m.getName() + i;
                        i++;
                    }
                    paths.put(m, s);
                }
            }

            return paths.get(method);
        }

        boolean isJAXRSParamAnnotation(Annotation annotation) {
            return annotation instanceof PathParam || annotation instanceof QueryParam || annotation instanceof CookieParam || annotation instanceof FormParam || annotation instanceof BeanParam || annotation instanceof HeaderParam || annotation instanceof MatrixParam;
        }

        boolean isJAXRSParamAnnotationUsedInMethod(Method method) {
            for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
                for (Annotation paramAnnotation : paramAnnotations) {
                    if (isJAXRSParamAnnotation(paramAnnotation)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public MethodVisitor visitMethod(final int access,
                final String name,
                String descriptor,
                final String signature,
                final String[] exceptions) {
            Method originalMethod = ASMUtils.getMethod(originalClass, name, descriptor);
            if (originalMethod == null) {
                throw new IllegalStateException("Method is not found in the original class");
            }

            MethodVisitor mv;
            Class<?> returnType = extractOriginalType(originalMethod.getReturnType());
            boolean hasResponse = returnType == Response.class;
            descriptor = hasResponse ? descriptor
                                     : Type.getMethodDescriptor(Type.getType(Response.class),
                                         Type.getArgumentTypes(descriptor));

            boolean allParametersIsPrimitive = true;
            Class<?>[] originalParameterTypes = originalMethod.getParameterTypes();
            int numOfParameters = originalParameterTypes.length;
            if (numOfParameters < MAX_PARAMETERS_COUNT_FOR_GET) {
                for (Class<?> parameterType : originalParameterTypes) {
                    if (!parameterType.isPrimitive()) {
                        allParametersIsPrimitive = false;
                        break;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("/").append(getPath(originalMethod));
            if (numOfParameters < MAX_PARAMETERS_COUNT_FOR_GET && allParametersIsPrimitive && !originalMethod
                .isAnnotationPresent(POST.class) || originalMethod.isAnnotationPresent(GET.class)) {
                mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                String[] parameterNames = resolveParameterNames(originalMethod);
                processAnnotationsOnMethodParameters(originalMethod, mv);
                addGetAnnotation(mv, originalMethod);
                if (!originalMethod.isAnnotationPresent(Path.class)) {
                    Set<String> usedPathParamValues = getUsedValuesInParamAnnotations(originalMethod,
                        e -> e instanceof PathParam,
                        e -> ((PathParam) e).value());
                    int i = 0;
                    for (String paramName : parameterNames) {
                        Annotation[] paramAnnotations = originalMethod.getParameterAnnotations()[i];
                        PathParam pathParam = null;
                        for (Annotation paramAnnotation : paramAnnotations) {
                            if (paramAnnotation instanceof PathParam) {
                                pathParam = (PathParam) paramAnnotation;
                            }
                        }
                        if (pathParam == null) {
                            String p = paramName;
                            int j = 1;
                            while (usedPathParamValues.contains(p)) {
                                p = paramName + j;
                            }
                            sb.append("/{").append(p).append(": .*}");
                            addPathParamAnnotation(mv, i, p);
                            usedPathParamValues.add(p);
                        } else {
                            sb.append("/{").append(pathParam.value()).append(": .*}");
                        }
                        i++;
                    }
                    addPathAnnotation(mv, originalMethod, sb.toString());
                } else {
                    Set<String> usedQueryParamValues = getUsedValuesInParamAnnotations(originalMethod,
                        e -> e instanceof QueryParam,
                        e -> ((QueryParam) e).value());
                    int i = 0;
                    for (String paramName : parameterNames) {
                        Annotation[] paramAnnotations = originalMethod.getParameterAnnotations()[i];
                        boolean jaxrsAnnotationPresented = false;
                        for (Annotation annotation : paramAnnotations) {
                            if (isJAXRSParamAnnotation(annotation)) {
                                jaxrsAnnotationPresented = true;
                                break;
                            }
                        }
                        if (!jaxrsAnnotationPresented) {
                            String p = paramName;
                            int j = 1;
                            while (usedQueryParamValues.contains(p)) {
                                p = paramName + j;
                            }
                            addQueryParamAnnotation(mv, i, p);
                            usedQueryParamValues.add(p);
                        }
                        i++;
                    }
                }
            } else {
                try {
                    if (numOfParameters > 1) {
                        if (!isJAXRSParamAnnotationUsedInMethod(originalMethod)) {
                            mv = super.visitMethod(access,
                                name,
                                changedParameterTypesDescription(descriptor, originalMethod),
                                signature,
                                exceptions);
                        } else {
                            mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                            processAnnotationsOnMethodParameters(originalMethod, mv);
                        }
                    } else {
                        mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        processAnnotationsOnMethodParameters(originalMethod, mv);
                    }
                    if (!hasResponse) {
                        annotateReturnElementClass(mv, returnType);
                    }
                    if (!originalMethod.isAnnotationPresent(POST.class)) {
                        addPostAnnotation(mv, originalMethod);
                    }
                    addPathAnnotation(mv, originalMethod, sb.toString());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            addConsumerProducesMethodAnnotations(mv, returnType, originalParameterTypes, originalMethod);
            addSwaggerMethodAnnotation(mv, originalMethod, getNickName(originalMethod));
            return mv;
        }

        private Set<String> getUsedValuesInParamAnnotations(Method originalMethod,
                Predicate<Annotation> predicate,
                Function<Annotation, String> func) {
            Set<String> usedPathParamValues = new HashSet<>();
            for (Annotation[] paramAnnotations : originalMethod.getParameterAnnotations()) {
                for (Annotation paramAnnotation : paramAnnotations) {
                    if (predicate.test(paramAnnotation)) {
                        usedPathParamValues.add(func.apply(paramAnnotation));
                        break;
                    }
                }
            }
            return usedPathParamValues;
        }

        private String[] resolveParameterNames(Method originalMethod) {
            return resolveMethodParameterNames ? MethodUtils
                .getParameterNames(openClass, originalMethod, provideRuntimeContext, provideVariations)
                                               : GenUtils.getParameterNames(originalMethod);
        }

        private boolean isTextMediaType(Class<?> type) {
            if (type.isPrimitive()) {
                return true;
            }
            for (Class<?> cl : TEXT_MEDIA_TYPE_SET) {
                if (cl.isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        }

        private void addConsumerProducesMethodAnnotations(MethodVisitor mv,
                Class<?> returnType,
                Class<?>[] originalParameterTypes,
                Method originalMethod) {
            if (returnType != null && isTextMediaType(returnType) && !originalMethod
                .isAnnotationPresent(Produces.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Produces.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, MediaType.TEXT_PLAIN);
                av2.visitEnd();
                av.visitEnd();
            }
            if (originalParameterTypes.length == 1 && isTextMediaType(originalParameterTypes[0]) && !originalMethod
                .isAnnotationPresent(Consumes.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, MediaType.TEXT_PLAIN);
                av2.visitEnd();
                av.visitEnd();
            }
        }

        private void processAnnotationsOnMethodParameters(Method originalMethod, MethodVisitor mv) {
            int index = 0;
            for (Annotation[] annotations : originalMethod.getParameterAnnotations()) {
                for (Annotation annotation : annotations) {
                    AnnotationVisitor av = mv
                        .visitParameterAnnotation(index, Type.getDescriptor(annotation.annotationType()), true);
                    InterfaceTransformer.processAnnotation(annotation, av);
                }
                index++;
            }
        }

        private void annotateReturnElementClass(MethodVisitor mv, Class<?> returnType) {
            if (Object.class == returnType || Void.class == returnType) {
                return;
            }
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ElementClass.class), true);
            av.visit("response", Type.getType(returnType));
            av.visitEnd();
        }

        private void addPostAnnotation(MethodVisitor mv, Method originalMethod) {
            if (!originalMethod.isAnnotationPresent(POST.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(POST.class), true);
                av.visitEnd();
            }
        }

        private void addGetAnnotation(MethodVisitor mv, Method originalMethod) {
            if (!originalMethod.isAnnotationPresent(GET.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(GET.class), true);
                av.visitEnd();
            }
        }

        private void addPathAnnotation(MethodVisitor mv, Method originalMethod, String path) {
            if (!originalMethod.isAnnotationPresent(Path.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Path.class), true);
                av.visit("value", path);
                av.visitEnd();
            }
        }

        private void addSwaggerMethodAnnotation(MethodVisitor mv, Method originalMethod, String nickname) {
            if (!originalMethod.isAnnotationPresent(ApiOperation.class) || !originalMethod
                .isAnnotationPresent(Operation.class)) {
                String summary = originalMethod.getReturnType().getSimpleName() + " " + MethodUtil
                    .printMethod(originalMethod.getName(), originalMethod.getParameterTypes(), true);

                IOpenMethod openMethod = MethodUtils.findRulesMethod(openClass, originalMethod);
                String detailedSummary = openMethod != null ? openMethod.getType()
                    .getDisplayName(INamedThing.LONG) + " " + MethodUtil.printSignature(openMethod, INamedThing.LONG)
                                                            : originalMethod.getReturnType()
                                                                .getTypeName() + " " + MethodUtil.printMethod(
                                                                    originalMethod.getName(),
                                                                    originalMethod.getParameterTypes(),
                                                                    false);
                if (!originalMethod.isAnnotationPresent(ApiOperation.class)) {
                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ApiOperation.class), true);
                    av.visit("value", summary.substring(0, Math.min(summary.length(), 120)));
                    av.visit("notes", (openMethod != null ? "Rules method: " : "Method: ") + detailedSummary);
                    av.visit("response", Type.getType(extractOriginalType(originalMethod.getReturnType())));
                    av.visit("nickname", nickname);
                    av.visitEnd();
                }
                if (!originalMethod.isAnnotationPresent(Operation.class)) {
                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Operation.class), true);
                    av.visit("operationId", nickname);
                    av.visit("summary", summary.substring(0, Math.min(summary.length(), 120)));
                    av.visit("description", (openMethod != null ? "Rules method: " : "Method: ") + detailedSummary);
                    Class<?> t = originalMethod.getReturnType();
                    int dim = 0;
                    while (t.isArray()) {
                        t = t.getComponentType();
                        dim++;
                    }
                    if (!originalMethod.isAnnotationPresent(ApiResponse.class) && dim < 2) {
                        AnnotationVisitor av1 = av.visitArray("responses");
                        AnnotationVisitor av2 = av1.visitAnnotation("responses", Type.getDescriptor(ApiResponse.class));
                        av2.visit("responseCode", String.valueOf(Response.Status.OK.getStatusCode()));
                        av2.visit("description", "Successful operation");
                        AnnotationVisitor av3 = av2.visitArray("content");
                        AnnotationVisitor av4 = av3.visitAnnotation("responses", Type.getDescriptor(Content.class));
                        addSchemaOpenApiAnnotation(av4, originalMethod.getReturnType());
                        av4.visitEnd();
                        av3.visitEnd();
                        av2.visitEnd();
                        av1.visitEnd();
                    }

                    av.visitEnd();
                }
            }
        }

        private void addSchemaOpenApiAnnotation(AnnotationVisitor av, Class<?> type) {
            boolean isArrayOrCollection = type.isArray() || Collection.class.isAssignableFrom(type);
            if (isArrayOrCollection) {
                av = av.visitAnnotation("array", Type.getDescriptor(ArraySchema.class));
                type = Collection.class.isAssignableFrom(type) ? Object.class : type.getComponentType();
            }
            final Class<?> extractedType = extractOriginalType(type);
            if (extractedType != null) {
                type = extractedType;
            }
            AnnotationVisitor av1 = av.visitAnnotation("schema", Type.getDescriptor(Schema.class));
            if (type == Integer.class || type == int.class || type == Short.class || type == short.class || type == Byte.class || type == byte.class) {
                av1.visit("type", "integer");
                av1.visit("format", "int32");
            } else if (type == Long.class || type == long.class) {
                av1.visit("type", "integer");
                av1.visit("format", "int64");
            } else if (type == Float.class || type == float.class) {
                av1.visit("type", "number");
                av1.visit("format", "float");
            } else if (type == Double.class || type == double.class) {
                av1.visit("type", "number");
                av1.visit("format", "double");
            } else if (type == Boolean.class || type == boolean.class) {
                av1.visit("type", "boolean");
            } else if (type == Character.class || type == char.class) {
                av1.visit("type", "string");
            } else if (Map.class.isAssignableFrom(type)) {
                av1.visit("implementation", Type.getType(Object.class)); // Impossible to define Map through Schema annotations.
            } else {
                av1.visit("implementation", Type.getType(type));
            }
            av1.visitEnd();
            if (isArrayOrCollection) {
                av.visitEnd();
            }
        }

        private void addPathParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(PathParam.class), true);
            av.visit("value", paramName);
            av.visitEnd();
        }

        private void addQueryParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(QueryParam.class), true);
            av.visit("value", paramName);
            av.visitEnd();
        }

        private void addProducesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Produces.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visitEnd();
            av.visitEnd();
        }

        private void addConsumesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visitEnd();
            av.visitEnd();
        }

        private void addSwaggerApiResponsesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(io.swagger.annotations.ApiResponses.class),
                true);
            AnnotationVisitor arrayAv = av.visitArray("value");
            addSwaggerApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.UNPROCESSABLE_ENTITY,
                UNPROCESSABLE_ENTITY_MESSAGE,
                UNPROCESSABLE_ENTITY_EXAMPLE);
            addSwaggerApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.BAD_REQUEST,
                BAD_REQUEST_MESSAGE,
                BAD_REQUEST_EXAMPLE);
            addSwaggerApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.INTERNAL_SERVER_ERROR_CODE,
                INTERNAL_SERVER_ERROR_MESSAGE,
                INTERNAL_SERVER_ERROR_EXAMPLE);
            arrayAv.visitEnd();
            av.visitEnd();
        }

        private void addSwaggerApiResponseAnnotation(AnnotationVisitor av,
                int code,
                String message,
                String jsonExample) {
            AnnotationVisitor apiResponseAv = av.visitAnnotation(null,
                Type.getDescriptor(io.swagger.annotations.ApiResponse.class));
            apiResponseAv.visit("code", code);
            apiResponseAv.visit("message", message);
            apiResponseAv.visit("response", Type.getType(JAXRSErrorResponse.class));

            AnnotationVisitor exampleAv = apiResponseAv.visitAnnotation("examples",
                Type.getDescriptor(io.swagger.annotations.Example.class));
            AnnotationVisitor exampleArrAv = exampleAv.visitArray("value");
            AnnotationVisitor examplePropAv = exampleArrAv.visitAnnotation(null,
                Type.getDescriptor(io.swagger.annotations.ExampleProperty.class));
            examplePropAv.visit("mediaType", MediaType.APPLICATION_JSON);
            examplePropAv.visit("value", jsonExample);
            examplePropAv.visitEnd();
            exampleArrAv.visitEnd();
            exampleAv.visitEnd();

            apiResponseAv.visitEnd();
        }

        private void addOpenApiResponsesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv
                .visitAnnotation(Type.getDescriptor(io.swagger.v3.oas.annotations.responses.ApiResponses.class), true);
            AnnotationVisitor arrayAv = av.visitArray("value");
            addOpenApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.UNPROCESSABLE_ENTITY,
                UNPROCESSABLE_ENTITY_MESSAGE,
                UNPROCESSABLE_ENTITY_EXAMPLE);
            addOpenApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.BAD_REQUEST,
                BAD_REQUEST_MESSAGE,
                BAD_REQUEST_EXAMPLE);
            addOpenApiResponseAnnotation(arrayAv,
                ExceptionResponseDto.INTERNAL_SERVER_ERROR_CODE,
                INTERNAL_SERVER_ERROR_MESSAGE,
                INTERNAL_SERVER_ERROR_EXAMPLE);
            arrayAv.visitEnd();
            av.visitEnd();
        }

        private void addOpenApiResponseAnnotation(AnnotationVisitor av, int code, String message, String jsonExample) {
            AnnotationVisitor apiResponseAv = av.visitAnnotation(null,
                Type.getDescriptor(io.swagger.v3.oas.annotations.responses.ApiResponse.class));
            apiResponseAv.visit("responseCode", String.valueOf(code));
            apiResponseAv.visit("description", message);

            AnnotationVisitor contentArrayAv = apiResponseAv.visitArray("content");
            AnnotationVisitor contentAv = contentArrayAv.visitAnnotation(null, Type.getDescriptor(Content.class));
            contentAv.visit("mediaType", MediaType.APPLICATION_JSON);

            AnnotationVisitor schemaAv = contentAv.visitAnnotation("schema", Type.getDescriptor(Schema.class));
            schemaAv.visit("implementation", Type.getType(JAXRSErrorResponse.class));
            schemaAv.visitEnd();

            AnnotationVisitor examplesArrAv = contentAv.visitArray("examples");
            AnnotationVisitor exampleObjectAv = examplesArrAv.visitAnnotation(null,
                Type.getDescriptor(ExampleObject.class));
            exampleObjectAv.visit("value", jsonExample);
            exampleObjectAv.visitEnd();
            examplesArrAv.visitEnd();

            contentAv.visitEnd();
            contentArrayAv.visitEnd();
            apiResponseAv.visitEnd();
        }

        private Class<?> extractOriginalType(Class<?> type) {
            Class<?> extractedType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(type);
            return extractedType == null ? type : extractedType;
        }

    }

    public static Map<Method, Method> buildMethodMap(Class<?> serviceClass,
            Class<?> enhancedServiceClass) throws Exception {
        Map<Method, Method> methodMap = new HashMap<>();
        for (Method method : enhancedServiceClass.getMethods()) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            } catch (NoSuchMethodException ex) {
                if (parameterTypes.length == 1) {
                    Class<?> methodArgument = parameterTypes[0];
                    parameterTypes = (Class<?>[]) methodArgument.getMethod("_types").invoke(null);
                }
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            }
        }
        return methodMap;
    }

    public static Class<?> enhanceInterface(Class<?> originalClass,
            IOpenClass openClass,
            ClassLoader classLoader,
            String serviceName,
            String serviceExposedUrl,
            boolean resolveMethodParameterNames,
            boolean provideRuntimeContext,
            boolean provideVariations) throws Exception {
        if (!originalClass.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported");
        }
        final String enhancedClassName = originalClass.getName() + DECORATED_CLASS_NAME_SUFFIX;
        Class<?> enhancedClass;
        try {
            enhancedClass = classLoader.loadClass(enhancedClassName);
        } catch (ClassNotFoundException e) {
            ClassWriter cw = new ClassWriter(0);
            JAXRSInterfaceAnnotationEnhancerClassVisitor enhancerClassVisitor = new JAXRSInterfaceAnnotationEnhancerClassVisitor(
                cw,
                originalClass,
                openClass,
                classLoader,
                serviceName,
                serviceExposedUrl,
                resolveMethodParameterNames,
                provideRuntimeContext,
                provideVariations);
            InterfaceTransformer transformer = new InterfaceTransformer(originalClass,
                enhancedClassName,
                InterfaceTransformer.IGNORE_PARAMETER_ANNOTATIONS);
            transformer.accept(enhancerClassVisitor);
            cw.visitEnd();
            enhancedClass = ClassUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
        }
        return enhancedClass;
    }
}
