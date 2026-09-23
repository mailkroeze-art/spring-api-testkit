package dev.apitestkit.speccore.model;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Eén "operation" uit de OpenAPI-spec: één combinatie van HTTP-methode en pad, bijvoorbeeld
 * {@code POST /pets}. Dit is de genormaliseerde, opgeschoonde versie van wat er in de YAML stond --
 * de rest van het framework hoeft nooit meer met de ruwe swagger-parser-objecten te werken.
 *
 * @param operationId         de unieke naam van deze operation uit de spec (bijv. "createPet")
 * @param httpMethod          de HTTP-methode in hoofdletters (GET, POST, ...)
 * @param path                het URL-pad met eventuele {@code {parameter}}-placeholders
 * @param tags                de OpenAPI-tags waarmee deze operation gegroepeerd is (bepaalt de klassenaam bij codegen)
 * @param parameters          alle pad-, query- en headerparameters
 * @param requestBodyValue    de request body, of {@code null} als deze operation er geen heeft (gebruik {@link #requestBody()})
 * @param responses           alle gedocumenteerde responses, per statuscode
 * @param securitySchemeNames de beveiligingsschema's die voor deze operation gelden (leeg = geen beveiliging)
 */
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
