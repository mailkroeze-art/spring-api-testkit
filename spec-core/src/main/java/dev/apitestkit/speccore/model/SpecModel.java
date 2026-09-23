package dev.apitestkit.speccore.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SpecModel(
        List<OperationModel> operations,
        Map<String, SecuritySchemeModel> securitySchemes
) {
    public SpecModel {
        operations = operations == null ? List.of() : List.copyOf(operations);
        securitySchemes = securitySchemes == null ? Map.of() : Map.copyOf(securitySchemes);
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
