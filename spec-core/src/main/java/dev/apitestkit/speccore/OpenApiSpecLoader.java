package dev.apitestkit.speccore;

import dev.apitestkit.speccore.model.OperationModel;
import dev.apitestkit.speccore.model.ParameterModel;
import dev.apitestkit.speccore.model.RequestBodyModel;
import dev.apitestkit.speccore.model.ResponseModel;
import dev.apitestkit.speccore.model.SchemaModel;
import dev.apitestkit.speccore.model.SecuritySchemeModel;
import dev.apitestkit.speccore.model.SpecModel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leest een OpenAPI 3.x YAML-bestand in en zet het om naar het interne, genormaliseerde SpecModel.
 * $refs worden volledig geresolved door swagger-parser (resolveFully), zodat de rest van het
 * framework nooit met $ref-verwijzingen te maken krijgt.
 */
public class OpenApiSpecLoader {

    public SpecModel load(Path specPath) {
        if (!Files.exists(specPath)) {
            throw new SpecLoadException("OpenAPI-specbestand niet gevonden: " + specPath.toAbsolutePath());
        }

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(specPath.toUri().toString(), null, options);
        OpenAPI openApi = result.getOpenAPI();

        if (openApi == null) {
            String messages = result.getMessages() == null ? "" : String.join("; ", result.getMessages());
            throw new SpecLoadException("Kon OpenAPI-spec niet parsen: " + specPath + (messages.isBlank() ? "" : " (" + messages + ")"));
        }

        Map<String, SecuritySchemeModel> securitySchemes = convertSecuritySchemes(openApi);
        List<String> globalSecuritySchemeNames = extractSchemeNames(openApi.getSecurity());
        List<OperationModel> operations = convertOperations(openApi, globalSecuritySchemeNames);

        return new SpecModel(operations, securitySchemes);
    }

    private Map<String, SecuritySchemeModel> convertSecuritySchemes(OpenAPI openApi) {
        Map<String, SecuritySchemeModel> schemes = new LinkedHashMap<>();
        if (openApi.getComponents() == null || openApi.getComponents().getSecuritySchemes() == null) {
            return schemes;
        }
        openApi.getComponents().getSecuritySchemes().forEach((name, scheme) -> {
            String type = scheme.getType() == null ? null : scheme.getType().toString();
            String in = scheme.getIn() == null ? null : scheme.getIn().toString().toLowerCase();
            String httpScheme = scheme.getScheme();
            schemes.put(name, new SecuritySchemeModel(name, type, in, httpScheme));
        });
        return schemes;
    }

    private List<OperationModel> convertOperations(OpenAPI openApi, List<String> globalSecuritySchemeNames) {
        List<OperationModel> operations = new ArrayList<>();
        if (openApi.getPaths() == null) {
            return operations;
        }
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((httpMethod, operation) ->
                        operations.add(convertOperation(path, httpMethod.toString(), operation, globalSecuritySchemeNames))));
        return operations;
    }

    private OperationModel convertOperation(String path, String httpMethod, Operation operation, List<String> globalSecuritySchemeNames) {
        List<String> securitySchemeNames = operation.getSecurity() == null
                ? globalSecuritySchemeNames
                : extractSchemeNames(operation.getSecurity());

        List<ParameterModel> parameters = operation.getParameters() == null
                ? List.of()
                : operation.getParameters().stream().map(this::convertParameter).toList();

        RequestBodyModel requestBody = convertRequestBody(operation.getRequestBody());
        Map<String, ResponseModel> responses = convertResponses(operation);

        return new OperationModel(
                operation.getOperationId(),
                httpMethod,
                path,
                operation.getTags(),
                parameters,
                requestBody,
                responses,
                securitySchemeNames
        );
    }

    private List<String> extractSchemeNames(List<SecurityRequirement> requirements) {
        if (requirements == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (SecurityRequirement requirement : requirements) {
            names.addAll(requirement.keySet());
        }
        return names;
    }

    private ParameterModel convertParameter(Parameter parameter) {
        boolean required = Boolean.TRUE.equals(parameter.getRequired());
        return new ParameterModel(
                parameter.getName(),
                parameter.getIn(),
                required,
                convertSchema(parameter.getSchema())
        );
    }

    private RequestBodyModel convertRequestBody(RequestBody requestBody) {
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            return null;
        }
        Content content = requestBody.getContent();
        Map.Entry<String, MediaType> entry = preferJson(content);
        MediaType mediaType = entry.getValue();
        boolean required = Boolean.TRUE.equals(requestBody.getRequired());
        Object example = mediaType.getExample();
        if (example == null && mediaType.getSchema() != null) {
            example = mediaType.getSchema().getExample();
        }
        return new RequestBodyModel(required, entry.getKey(), convertSchema(mediaType.getSchema()), example);
    }

    private Map.Entry<String, MediaType> preferJson(Content content) {
        if (content.containsKey("application/json")) {
            return Map.entry("application/json", content.get("application/json"));
        }
        return content.entrySet().iterator().next();
    }

    private Map<String, ResponseModel> convertResponses(Operation operation) {
        Map<String, ResponseModel> responses = new LinkedHashMap<>();
        if (operation.getResponses() == null) {
            return responses;
        }
        operation.getResponses().forEach((statusCode, apiResponse) ->
                responses.put(statusCode, convertResponse(statusCode, apiResponse)));
        return responses;
    }

    private ResponseModel convertResponse(String statusCode, ApiResponse apiResponse) {
        boolean hasContent = apiResponse.getContent() != null && !apiResponse.getContent().isEmpty();
        SchemaModel schema = null;
        if (hasContent) {
            Map.Entry<String, MediaType> entry = preferJson(apiResponse.getContent());
            schema = convertSchema(entry.getValue().getSchema());
        }
        return new ResponseModel(statusCode, apiResponse.getDescription(), hasContent, schema);
    }

    @SuppressWarnings("unchecked")
    private SchemaModel convertSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }

        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            return mergeAllOf(schema);
        }

        Map<String, SchemaModel> properties = new LinkedHashMap<>();
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, propSchema) ->
                    properties.put((String) name, convertSchema((Schema<?>) propSchema)));
        }

        SchemaModel items = schema.getItems() != null ? convertSchema(schema.getItems()) : null;
        List<String> enumValues = schema.getEnum() == null
                ? List.of()
                : schema.getEnum().stream().map(String::valueOf).toList();

        return new SchemaModel(
                schema.getType(),
                schema.getFormat(),
                toBigDecimal(schema.getMinimum()),
                toBigDecimal(schema.getMaximum()),
                schema.getMinLength(),
                schema.getMaxLength(),
                schema.getRequired(),
                properties,
                items,
                enumValues,
                schema.getExample()
        );
    }

    private SchemaModel mergeAllOf(Schema<?> schema) {
        Map<String, SchemaModel> mergedProperties = new LinkedHashMap<>();
        List<String> mergedRequired = new ArrayList<>();
        String format = schema.getFormat();

        for (Object subSchemaObj : schema.getAllOf()) {
            Schema<?> subSchema = (Schema<?>) subSchemaObj;
            SchemaModel converted = convertSchema(subSchema);
            if (converted == null) {
                continue;
            }
            mergedProperties.putAll(converted.properties());
            mergedRequired.addAll(converted.required());
            if (format == null) {
                format = converted.format();
            }
        }

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, propSchema) ->
                    mergedProperties.put((String) name, convertSchema((Schema<?>) propSchema)));
        }
        if (schema.getRequired() != null) {
            mergedRequired.addAll(schema.getRequired());
        }

        return new SchemaModel(
                "object",
                format,
                null,
                null,
                null,
                null,
                mergedRequired,
                mergedProperties,
                null,
                List.of(),
                schema.getExample()
        );
    }

    private BigDecimal toBigDecimal(Number number) {
        if (number == null) {
            return null;
        }
        return new BigDecimal(number.toString());
    }
}
