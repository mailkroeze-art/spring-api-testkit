package dev.apitestkit.speccore.model;

/**
 * Eén gedocumenteerde mogelijke response van een operation, bijvoorbeeld "bij statuscode 404 hoort
 * deze beschrijving". Deze lijst van mogelijke responses is precies wat het framework gebruikt om
 * te bepalen of een statuscode die de API teruggeeft, wel "toegestaan" is volgens de spec.
 *
 * @param statusCode  de statuscode als tekst, bijv. "200" of "404"
 * @param description de beschrijving uit de spec
 * @param hasContent  of er een response body gedocumenteerd is
 * @param schema      de structuur van die response body, of {@code null} als er geen body is
 */
public record ResponseModel(
        String statusCode,
        String description,
        boolean hasContent,
        SchemaModel schema
) {
}
