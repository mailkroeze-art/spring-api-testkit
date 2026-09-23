package dev.apitestkit.speccore.model;

public record ParameterModel(
        String name,
        String in,
        boolean required,
        SchemaModel schema
) {
}
