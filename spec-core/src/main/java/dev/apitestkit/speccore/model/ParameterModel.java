package dev.apitestkit.speccore.model;

/**
 * Eén parameter van een operation, bijvoorbeeld de {@code limit} query-parameter of het
 * {@code petId} pad-onderdeel.
 *
 * @param name     de naam van de parameter zoals in de spec
 * @param in       waar de parameter hoort: "path", "query", "header" of "cookie"
 * @param required of deze parameter verplicht is
 * @param schema   het type en de eventuele beperkingen (min/max, enum, formaat, ...)
 */
public record ParameterModel(
        String name,
        String in,
        boolean required,
        SchemaModel schema
) {
}
