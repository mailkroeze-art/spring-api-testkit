package dev.apitestkit.speccore.model;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record OperationModel(
        String operationId,
        String httpMethod,
        String path,
        List<String> tags,
        List<ParameterModel> parameters,
        RequestBodyModel requestBodyValue,
        Map<String, ResponseModel> responses,
        List<String> securitySchemeNames
) {
    public OperationModel {
        tags = tags == null ? List.of() : List.copyOf(tags);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        responses = OrderedMaps.copyOf(responses);
        securitySchemeNames = securitySchemeNames == null ? List.of() : List.copyOf(securitySchemeNames);
    }

    public Optional<RequestBodyModel> requestBody() {
        return Optional.ofNullable(requestBodyValue);
    }

    public boolean requiresAuthentication() {
        return !securitySchemeNames.isEmpty();
    }
}
