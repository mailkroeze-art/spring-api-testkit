package dev.apitestkit.speccore.model;

public record ResponseModel(
        String statusCode,
        String description,
        boolean hasContent,
        SchemaModel schema
) {
}
