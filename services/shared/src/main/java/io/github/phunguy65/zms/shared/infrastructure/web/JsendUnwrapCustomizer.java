package io.github.phunguy65.zms.shared.infrastructure.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Strips {@link JsendResponse} envelope schemas from the generated OpenAPI spec.
 *
 * <p>Controllers return {@code ResponseEntity<JsendResponse<T>>}, so springdoc generates wrapper
 * schemas like {@code JsendResponseLoginResponse} with {@code {status, data, message}} fields.
 * Client-side interceptors ({@code JsendUnwrapInterceptor}) already strip the envelope at runtime,
 * so generated client types should reference the inner payload type directly.
 *
 * <p>This customizer:
 * <ol>
 *   <li>Finds all schemas whose name starts with {@code JsendResponse}.</li>
 *   <li>Extracts the {@code data} property's {@code $ref} as the inner payload type.</li>
 *   <li>Rewrites all response schema references from the wrapper to the inner type.</li>
 *   <li>For {@code JsendResponse<Void>} (no {@code data.$ref}), removes the response content.</li>
 *   <li>Removes all wrapper schema definitions from {@code components/schemas}.</li>
 * </ol>
 *
 * <p>Placed in the shared module and auto-discovered by all services via
 * {@code scanBasePackages = "io.github.phunguy65.zms.shared"}.
 */
@Component
public class JsendUnwrapCustomizer implements OpenApiCustomizer {

    private static final String JSEND_SCHEMA_PREFIX = "JsendResponse";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    @Override
    public void customise(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }

        Map<String, Schema> schemas = components.getSchemas();

        Map<String, String> wrapperToInnerRef = buildWrapperMapping(schemas);
        if (wrapperToInnerRef.isEmpty()) {
            return;
        }

        if (openApi.getPaths() != null) {
            for (PathItem pathItem : openApi.getPaths().values()) {
                rewritePathItem(pathItem, wrapperToInnerRef);
            }
        }

        for (String wrapperName : wrapperToInnerRef.keySet()) {
            schemas.remove(wrapperName);
        }
    }

    /**
     * Scans component schemas for {@code JsendResponse*} wrappers and maps each to the inner
     * payload type's {@code $ref}. If the {@code data} property has no {@code $ref} (e.g.
     * {@code JsendResponse<Void>}), the value is {@code null}.
     */
    private Map<String, String> buildWrapperMapping(Map<String, Schema> schemas) {
        Map<String, String> mapping = new HashMap<>();

        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(JSEND_SCHEMA_PREFIX)) {
                continue;
            }

            Schema wrapperSchema = entry.getValue();
            Map<String, Schema> properties = wrapperSchema.getProperties();
            if (properties == null || !properties.containsKey("data")) {
                continue;
            }

            Schema dataProperty = properties.get("data");
            String innerRef = dataProperty.get$ref();
            mapping.put(name, innerRef);
        }

        return mapping;
    }

    /** Rewrites response schemas across all HTTP operations in a {@link PathItem}. */
    private void rewritePathItem(PathItem pathItem, Map<String, String> wrapperToInnerRef) {
        for (Operation operation : allOperations(pathItem)) {
            rewriteOperation(operation, wrapperToInnerRef);
        }
    }

    /** Rewrites response schema references for a single {@link Operation}. */
    private void rewriteOperation(Operation operation, Map<String, String> wrapperToInnerRef) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }

        for (ApiResponse response : responses.values()) {
            Content content = response.getContent();
            if (content == null) {
                continue;
            }

            for (Map.Entry<String, MediaType> mediaEntry : content.entrySet()) {
                MediaType mediaType = mediaEntry.getValue();
                Schema schema = mediaType.getSchema();
                if (schema == null || schema.get$ref() == null) {
                    continue;
                }

                String ref = schema.get$ref();
                String schemaName = extractSchemaName(ref);
                if (schemaName == null || !wrapperToInnerRef.containsKey(schemaName)) {
                    continue;
                }

                String innerRef = wrapperToInnerRef.get(schemaName);
                if (innerRef != null) {
                    schema.set$ref(innerRef);
                } else {
                    response.setContent(null);
                    break;
                }
            }
        }
    }

    /**
     * Extracts the schema name from a {@code $ref} string.
     *
     * @param ref e.g. {@code "#/components/schemas/JsendResponseLoginResponse"}
     * @return e.g. {@code "JsendResponseLoginResponse"}, or {@code null} if not a schema ref
     */
    private String extractSchemaName(String ref) {
        if (ref != null && ref.startsWith(SCHEMA_REF_PREFIX)) {
            return ref.substring(SCHEMA_REF_PREFIX.length());
        }
        return null;
    }

    /** Collects all non-null {@link Operation}s from a {@link PathItem}. */
    private List<Operation> allOperations(PathItem pathItem) {
        return Stream.of(
                        pathItem.getGet(),
                        pathItem.getPost(),
                        pathItem.getPut(),
                        pathItem.getPatch(),
                        pathItem.getDelete(),
                        pathItem.getHead(),
                        pathItem.getOptions(),
                        pathItem.getTrace())
                .filter(op -> op != null)
                .toList();
    }
}
