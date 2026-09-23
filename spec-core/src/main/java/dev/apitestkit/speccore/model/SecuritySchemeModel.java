package dev.apitestkit.speccore.model;

/**
 * type: "apiKey" of "http"; in: header/query/cookie voor apiKey; scheme: bearer/basic voor http.
 */
public record SecuritySchemeModel(
        String name,
        String type,
        String in,
        String scheme
) {
}
