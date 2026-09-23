package dev.apitestkit.speccore.model;

import java.util.Optional;

public record RequestBodyModel(
        boolean required,
        String contentType,
        SchemaModel schema,
        Object exampleValue
) {
    public Optional<Object> example() {
        return Optional.ofNullable(exampleValue);
    }
}
