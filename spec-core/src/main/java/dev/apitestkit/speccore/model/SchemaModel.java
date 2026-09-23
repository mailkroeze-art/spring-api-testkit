package dev.apitestkit.speccore.model;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * De beschrijving van hoe één stukje data eruit moet zien: een "schema" in OpenAPI-taal. Dit kan
 * een heel object zijn (met eigen velden in {@code properties}), een lijst ({@code items}
 * beschrijft dan wat er in de lijst zit), of een simpele waarde zoals tekst of een getal.
 * Deze klasse is wat {@link dev.apitestkit.speccore.generate.ExampleValueGenerator} gebruikt om
 * testwaarden te verzinnen, en wat {@link dev.apitestkit.speccore.generate.TestCaseGenerator}
 * gebruikt om te bepalen welke testcases zinvol zijn (bijv. alleen grenswaarde-tests als er een
 * minimum/maximum is opgegeven).
 *
 * @param type       het datatype: "string", "integer", "number", "boolean", "array" of "object"
 * @param format     een extra beperking op het type, bijv. "uuid", "email", "date-time"
 * @param minimum    de kleinst toegestane waarde (voor getallen)
 * @param maximum    de grootst toegestane waarde (voor getallen)
 * @param minLength  de kortste toegestane lengte (voor tekst)
 * @param maxLength  de langste toegestane lengte (voor tekst)
 * @param required   welke velden van een object verplicht zijn
 * @param properties de velden van een object, per naam
 * @param items      het schema van de elementen in een lijst (alleen bij type "array")
 * @param enumValues de toegestane waarden, als dit veld een vaste lijst van opties heeft
 * @param example    een voorbeeldwaarde uit de spec zelf, indien aanwezig
 */
public record SchemaModel(
        String type,
        String format,
        BigDecimal minimum,
        BigDecimal maximum,
        Integer minLength,
        Integer maxLength,
        List<String> required,
        Map<String, SchemaModel> properties,
        SchemaModel items,
        List<String> enumValues,
        Object example
) {
    public SchemaModel {
        required = required == null ? List.of() : List.copyOf(required);
        properties = OrderedMaps.copyOf(properties);
        enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
    }

    public static SchemaModel empty(String type) {
        return new SchemaModel(type, null, null, null, null, null, null, null, null, null, null);
    }
}
