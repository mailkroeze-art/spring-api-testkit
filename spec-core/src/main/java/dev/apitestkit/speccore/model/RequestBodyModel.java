package dev.apitestkit.speccore.model;

import java.util.Optional;

/**
 * De request body van een operation (bijvoorbeeld de JSON die je meestuurt bij het aanmaken van
 * een huisdier). Alleen {@code application/json} wordt op dit moment ondersteund.
 *
 * @param required     of de body verplicht is
 * @param contentType  het content-type uit de spec (bijv. "application/json")
 * @param schema       de structuur van de body: welke velden, welk type, wat is verplicht
 * @param exampleValue een voorbeeldwaarde uit de spec zelf, of {@code null} als er geen is (gebruik {@link #example()})
 */
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
