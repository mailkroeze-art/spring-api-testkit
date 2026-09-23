package dev.apitestkit.speccore.model;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * De volledige, opgeschoonde inhoud van één OpenAPI-spec: alle operations en alle
 * beveiligingsschema's. Dit is wat {@link dev.apitestkit.speccore.OpenApiSpecLoader} teruggeeft,
 * en wat de rest van het framework als startpunt gebruikt om testcases uit te genereren.
 *
 * @param operations       alle endpoints (methode + pad) uit de spec
 * @param securitySchemes  alle gedefinieerde manieren van inloggen (apiKey, bearer, basic, ...), per naam
 */
public record SpecModel(
        List<OperationModel> operations,
        Map<String, SecuritySchemeModel> securitySchemes
) {
    public SpecModel {
        operations = operations == null ? List.of() : List.copyOf(operations);
        securitySchemes = OrderedMaps.copyOf(securitySchemes);
    }

    public Optional<OperationModel> operationById(String operationId) {
        return operations.stream()
                .filter(o -> o.operationId() != null && o.operationId().equals(operationId))
                .findFirst();
    }

    public Optional<SecuritySchemeModel> securityScheme(String name) {
        return Optional.ofNullable(securitySchemes.get(name));
    }
}
