package org.openl.rules.spring.openapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.tags.Tag;
import org.openl.util.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class OpenApiSpringMvcReader {

    private final SpringMvcHandlerMethodsHelper handlerMethodsHelper;

    public OpenApiSpringMvcReader(SpringMvcHandlerMethodsHelper handlerMethodsHelper) {
        this.handlerMethodsHelper = handlerMethodsHelper;
    }

    public void read(OpenApiContext openApiContext, Map<String, Class<?>> controllers) {
        handlerMethodsHelper.getHandlerMethods()
            .entrySet()
            .stream()
            .filter(e -> isRestControllers(e.getValue(), controllers))
            .filter(e -> !isHiddenApiMethod(e.getValue().getMethod()))
            .forEach(e -> visitHandlerMethod(openApiContext, e.getKey(), e.getValue()));
    }

    private void visitHandlerMethod(OpenApiContext openApiContext,
            RequestMappingInfo mappingInfo,
            HandlerMethod method) {
        Function<Set<MediaType>, String[]> mediaTypesToStringArray = set -> set.stream()
            .map(Object::toString)
            .toArray(String[]::new);
        final var consumesMediaTypes = mediaTypesToStringArray
            .apply(mappingInfo.getConsumesCondition().getConsumableMediaTypes());
        final var producesMediaTypes = mediaTypesToStringArray
            .apply(mappingInfo.getProducesCondition().getProducibleMediaTypes());
        for (String pathPattern : mappingInfo.getPatternsCondition().getPatterns()) {
            // Map<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operationMap = null;
            // if (paths.containsKey(pathPattern)) {
            // operationMap = paths.get(pathPattern).readOperationsMap();
            // }
            for (RequestMethod requestMethod : mappingInfo.getMethodsCondition().getMethods()) {
                parseMethod(openApiContext,
                    mappingInfo,
                    method,
                    pathPattern,
                    requestMethod,
                    consumesMediaTypes,
                    producesMediaTypes);
            }
        }
        if (openApiContext.getOpenAPI().getTags() != null) {
            openApiContext.getOpenAPI().getTags().sort(Comparator.comparing(Tag::getName));
        }
    }

    private void parseMethod(OpenApiContext apiContext,
            RequestMappingInfo mappingInfo,
            HandlerMethod method,
            String pathPattern,
            RequestMethod requestMethod,
            String[] consumesMediaTypes,
            String[] producesMediaTypes) {
        final var operation = new Operation();
        var apiOperation = ReflectionUtils.getAnnotation(method.getMethod(),
            io.swagger.v3.oas.annotations.Operation.class);

        JsonView jsonViewAnnotation = null;
        JsonView jsonViewAnnotationForRequestBody = null;
        if (apiOperation == null || !apiOperation.ignoreJsonView()) {
            jsonViewAnnotation = ReflectionUtils.getAnnotation(method.getMethod(), JsonView.class);
            jsonViewAnnotationForRequestBody = (JsonView) Arrays
                .stream(ReflectionUtils.getParameterAnnotations(method.getMethod()))
                .filter(arr -> Arrays.stream(arr)
                    .anyMatch(annotation -> annotation.annotationType()
                        .equals(io.swagger.v3.oas.annotations.parameters.RequestBody.class)))
                .flatMap(Arrays::stream)
                .filter(annotation -> annotation.annotationType().equals(JsonView.class))
                .reduce((a, b) -> null)
                .orElse(jsonViewAnnotation);
        }

        if (isDeprecatedMethod(method.getMethod())) {
            operation.setDeprecated(true);
        }

        if (StringUtils.isBlank(operation.getOperationId())) {
            operation.setOperationId(getOperationId(apiContext, method.getMethod().getName()));
        }

        // parse OpenAPI Tags annotations
        parseMethodTags(apiContext, method, operation);

        // TODO parse OpenAPI Parameter annotation
        var apiParameters = ReflectionUtils.getRepeatableAnnotations(method.getMethod(),
            io.swagger.v3.oas.annotations.Parameter.class);
        parseParameters(apiContext, apiParameters, consumesMediaTypes, operation, jsonViewAnnotation)
            .forEach(operation::addParametersItem);

        // parse OpenAPI RequestBody annotation
        var apiRequestBody = ReflectionUtils.getAnnotation(method.getMethod(),
            io.swagger.v3.oas.annotations.parameters.RequestBody.class);
        if (apiRequestBody != null && operation.getRequestBody() == null) {
            parseRequestBody(apiContext, apiRequestBody, consumesMediaTypes, jsonViewAnnotation)
                .ifPresent(operation::setRequestBody);
        }

        // parse OpenAPI ApiResponses annotation on class level
        var classResponses = apiContext.getClassApiResponses(method.getBeanType());
        if (classResponses == null) {
            var classApiResponses = ReflectionUtils.getRepeatableAnnotations(method.getBeanType(),
                io.swagger.v3.oas.annotations.responses.ApiResponse.class);
            if (classApiResponses != null) {
                var responses = parseApiResponses(apiContext,
                    classApiResponses,
                    producesMediaTypes,
                    jsonViewAnnotation);
                if (responses.isPresent()) {
                    apiContext.addClassApiResponses(method.getBeanType(), responses.get());
                    classResponses = responses.get();
                }
            }
        }
        if (classResponses != null) {
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
                operation.getResponses().putAll(classResponses);
            } else {
                classResponses.forEach(operation.getResponses()::addApiResponse);
            }
        }

        // parse OpenAPI Operation annotation
        parseOperation(apiContext, apiOperation, operation, consumesMediaTypes, producesMediaTypes, jsonViewAnnotation);

        // parse response body
        var apiResponses = ReflectionUtils.getRepeatableAnnotations(method.getMethod(),
            io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        if (apiResponses != null) {
            parseApiResponses(apiContext, apiResponses, producesMediaTypes, jsonViewAnnotation).ifPresent(responses -> {
                if (operation.getResponses() == null) {
                    operation.setResponses(responses);
                } else {
                    responses.forEach(operation.getResponses()::addApiResponse);
                }
            });
        }

        // register parsed operation method
        PathItem pathItem;
        if (apiContext.getPaths().containsKey(pathPattern)) {
            pathItem = apiContext.getPaths().get(pathPattern);
        } else {
            pathItem = new PathItem();
            apiContext.getPaths().addPathItem(pathPattern, pathItem);
        }
        pathItem.operation(PathItem.HttpMethod.valueOf(requestMethod.name()), operation);
    }

    private Optional<ApiResponses> parseApiResponses(OpenApiContext apiContext,
            List<io.swagger.v3.oas.annotations.responses.ApiResponse> apiResponses,
            String[] produces,
            JsonView jsonViewAnnotation) {
        if (apiResponses == null) {
            return Optional.empty();
        }
        var responses = new ApiResponses();
        for (var apiResponse : apiResponses) {
            var response = new ApiResponse();
            if (StringUtils.isNotBlank(apiResponse.ref())) {
                response.set$ref(apiResponse.ref());
                if (StringUtils.isNotBlank(apiResponse.responseCode())) {
                    responses.addApiResponse(apiResponse.responseCode(), response);
                } else {
                    responses._default(response);
                }
                continue;
            }
            if (StringUtils.isNotBlank(apiResponse.description())) {
                response.setDescription(apiResponse.description());
            }
            if (apiResponse.extensions().length > 0) {
                AnnotationsUtils.getExtensions(apiResponse.extensions()).forEach(response::addExtension);
            }

            AnnotationsUtils
                .getContent(apiResponse
                    .content(), new String[0], produces, null, apiContext.getComponents(), jsonViewAnnotation)
                .ifPresent(response::content);
            AnnotationsUtils.getHeaders(apiResponse.headers(), jsonViewAnnotation).ifPresent(response::headers);
            if (StringUtils.isNotBlank(response.getDescription()) || response.getContent() != null || response
                .getHeaders() != null) {
                var links = AnnotationsUtils.getLinks(apiResponse.links());
                if (links.size() > 0) {
                    response.setLinks(links);
                }
                if (StringUtils.isNotBlank(apiResponse.responseCode())) {
                    responses.addApiResponse(apiResponse.responseCode(), response);
                } else {
                    responses._default(response);
                }
            }
        }

        if (responses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(responses);
    }

    private Optional<RequestBody> parseRequestBody(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.parameters.RequestBody apiRequestBody,
            String[] consumes,
            JsonView jsonViewAnnotation) {
        if (apiRequestBody == null) {
            return Optional.empty();
        }
        RequestBody requestBody = new RequestBody();
        boolean empty = true;

        if (StringUtils.isNotBlank(apiRequestBody.ref())) {
            requestBody.set$ref(apiRequestBody.ref());
            return Optional.of(requestBody);
        }
        if (StringUtils.isNotBlank(apiRequestBody.description())) {
            requestBody.setDescription(apiRequestBody.description());
            empty = false;
        }
        if (apiRequestBody.required()) {
            requestBody.setRequired(true);
            empty = false;
        }
        if (apiRequestBody.extensions().length > 0) {
            AnnotationsUtils.getExtensions(apiRequestBody.extensions()).forEach(requestBody::addExtension);
            empty = false;
        }

        if (apiRequestBody.content().length > 0) {
            empty = false;
        }

        if (empty) {
            return Optional.empty();
        }
        AnnotationsUtils
            .getContent(apiRequestBody
                .content(), new String[0], consumes, null, apiContext.getComponents(), jsonViewAnnotation)
            .ifPresent(requestBody::setContent);
        return Optional.of(requestBody);
    }

    private List<Parameter> parseParameters(OpenApiContext apiContext,
            List<io.swagger.v3.oas.annotations.Parameter> apiParameters,
            String[] consumes,
            Operation operation,
            JsonView jsonViewAnnotation) {

        if (apiParameters == null) {
            return Collections.emptyList();
        }

        List<Parameter> parameters = new ArrayList<>();
        for (var apiParameter : apiParameters) {
            if (apiParameter.hidden()) {
                continue;
            }
            parameters.add(parseParameter(apiContext, apiParameter, consumes, jsonViewAnnotation));
        }

        return parameters;
    }

    private Parameter parseParameter(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.Parameter apiParameter,
            String[] consumes,
            JsonView jsonViewAnnotation) {
        var parameter = new Parameter();

        if (StringUtils.isNotBlank(apiParameter.ref())) {
            parameter.set$ref(apiParameter.ref());
            return parameter;
        }

        var type = ParameterProcessor.getParameterType(apiParameter);
        ParameterProcessor.applyAnnotations(parameter,
            type,
            Collections.singletonList(apiParameter),
            apiContext.getComponents(),
            new String[0],
            consumes,
            jsonViewAnnotation);

        return parameter;
    }

    private void parseOperation(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.Operation apiOperation,
            Operation operation,
            String[] consumes,
            String[] produces,
            JsonView jsonViewAnnotation) {
        if (apiOperation == null) {
            return;
        }
        if (StringUtils.isNotBlank(apiOperation.summary())) {
            operation.setSummary(apiOperation.summary());
        }
        if (StringUtils.isNotBlank(apiOperation.description())) {
            operation.setDescription(apiOperation.description());
        }
        if (StringUtils.isNotBlank(apiOperation.operationId())) {
            operation.setOperationId(getOperationId(apiContext, apiOperation.operationId()));
        }
        if (apiOperation.deprecated()) {
            operation.setDeprecated(Boolean.TRUE);
        }
        if (apiOperation.tags() != null) {
            Stream.of(apiOperation.tags())
                .filter(tag -> operation.getTags() == null || !operation.getTags().contains(tag))
                .forEach(operation::addTagsItem);
        }

        parseApiResponses(apiContext, Arrays.asList(apiOperation.responses()), produces, jsonViewAnnotation)
            .ifPresent(responses -> {
                if (operation.getResponses() == null) {
                    operation.setResponses(responses);
                } else {
                    responses.forEach(operation.getResponses()::addApiResponse);
                }
            });

        parseParameters(apiContext, Arrays.asList(apiOperation.parameters()), consumes, operation, jsonViewAnnotation)
            .forEach(operation::addParametersItem);

        if (apiOperation.requestBody() != null && operation.getRequestBody() == null) {
            parseRequestBody(apiContext, apiOperation.requestBody(), consumes, jsonViewAnnotation).ifPresent(operation::setRequestBody);
        }
    }

    // TODO: Parse Callback, ExternalDocumentation, SecurityRequirement, Server
    private void parseMethodTags(OpenApiContext openApiContext, HandlerMethod method, Operation operation) {
        var typeTags = openApiContext.getClassTags(method.getBeanType());
        if (typeTags == null) {
            processTagsFromType(openApiContext, method.getBeanType());
            typeTags = openApiContext.getClassTags(method.getBeanType());
        }
        List<Stream<io.swagger.v3.oas.annotations.tags.Tag>> tags = new ArrayList<>();
        Optional
            .ofNullable(
                AnnotationUtils.findAnnotation(method.getMethod(), io.swagger.v3.oas.annotations.tags.Tags.class))
            .ifPresent(anno -> tags.add(Stream.of(anno.value())));
        Optional
            .ofNullable(
                AnnotationUtils.findAnnotation(method.getMethod(), io.swagger.v3.oas.annotations.tags.Tag.class))
            .ifPresent(anno -> tags.add(Stream.of(anno)));
        AnnotationsUtils
            .getTags(tags.stream().flatMap(Function.identity()).toArray(io.swagger.v3.oas.annotations.tags.Tag[]::new),
                false)
            .stream()
            .flatMap(Collection::stream)
            .forEach(tagItem -> {
                openApiContext.addTagsItem(tagItem);
                if (operation.getTags() == null || !operation.getTags().contains(tagItem.getName())) {
                    operation.addTagsItem(tagItem.getName());
                }
            });

        Optional.ofNullable(typeTags)
            .map(Map::keySet)
            .stream()
            .flatMap(Collection::stream)
            .filter(tag -> operation.getTags() == null || !operation.getTags().contains(tag))
            .forEach(operation::addTagsItem);
    }

    private void processTagsFromType(OpenApiContext openApiContext, Class<?> beanType) {
        List<Stream<io.swagger.v3.oas.annotations.tags.Tag>> tags = new ArrayList<>();
        Optional
            .ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.OpenAPIDefinition.class))
            .map(io.swagger.v3.oas.annotations.OpenAPIDefinition::tags)
            .ifPresent(anno -> tags.add(Stream.of(anno)));
        Optional.ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.tags.Tags.class))
            .ifPresent(anno -> tags.add(Stream.of(anno.value())));
        Optional.ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.tags.Tag.class))
            .ifPresent(anno -> tags.add(Stream.of(anno)));

        AnnotationsUtils
            .getTags(tags.stream().flatMap(Function.identity()).toArray(io.swagger.v3.oas.annotations.tags.Tag[]::new),
                false)
            .ifPresent(res -> openApiContext.addClassTags(beanType, res));
    }

    protected String getOperationId(OpenApiContext apiContext, String operationId) {
        boolean operationIdUsed = existOperationId(apiContext, operationId);
        String operationIdToFind = null;
        int counter = 0;
        while (operationIdUsed) {
            operationIdToFind = String.format("%s_%d", operationId, ++counter);
            operationIdUsed = existOperationId(apiContext, operationIdToFind);
        }
        if (operationIdToFind != null) {
            operationId = operationIdToFind;
        }
        return operationId;
    }

    private boolean existOperationId(OpenApiContext apiContext, String operationId) {
        if (apiContext.getPaths() == null) {
            return false;
        }
        for (var path : apiContext.getPaths().values()) {
            var pathOperationIds = extractOperationIdFromPathItem(path);
            if (pathOperationIds.contains(operationId)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractOperationIdFromPathItem(PathItem path) {
        return Stream
            .of(path.getGet(),
                path.getPost(),
                path.getPut(),
                path.getDelete(),
                path.getOptions(),
                path.getHead(),
                path.getPatch())
            .filter(Objects::nonNull)
            .map(Operation::getOperationId)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private static boolean isRestControllers(HandlerMethod method, Map<String, Class<?>> controllers) {
        return isRestController(method.getBeanType()) && controllers.get(method.getBean().toString()) == method
            .getBeanType() && hasResponseBody(method);
    }

    private static boolean isRestController(Class<?> cl) {
        return AnnotationUtils.findAnnotation(cl, RestController.class) != null || (AnnotationUtils.findAnnotation(cl,
            Controller.class) != null && AnnotationUtils.findAnnotation(cl, ResponseBody.class) != null);
    }

    private static boolean hasResponseBody(HandlerMethod method) {
        return method.hasMethodAnnotation(
            ResponseBody.class) || AnnotationUtils.findAnnotation(method.getBeanType(), ResponseBody.class) != null;
    }

    private static boolean isHiddenApiMethod(Method method) {
        var anno = AnnotationUtils.findAnnotation(method, io.swagger.v3.oas.annotations.Operation.class);
        return anno != null && anno.hidden() || AnnotationUtils.findAnnotation(method,
            io.swagger.v3.oas.annotations.Hidden.class) != null || AnnotationUtils
                .findAnnotation(method.getDeclaringClass(), io.swagger.v3.oas.annotations.Hidden.class) != null;
    }

    private static boolean isDeprecatedMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, Deprecated.class) != null || AnnotationUtils
            .findAnnotation(method.getDeclaringClass(), Deprecated.class) != null;
    }
}
