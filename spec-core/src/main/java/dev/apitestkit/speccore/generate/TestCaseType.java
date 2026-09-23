package dev.apitestkit.speccore.generate;

/**
 * Elk type testcase dat het framework automatisch kan genereren -- zie de tabel "Casetypes" in
 * README.md voor een uitleg per type in gewone taal. Het getal achter elke naam hieronder is de
 * standaard verwachte statuscode; die is overschrijfbaar per operation via
 * {@code expectedStatusOverrides} in test-config.yaml.
 */
public enum TestCaseType {
    /** Een geldige aanvraag conform de spec -- verwacht de eerste gedocumenteerde 2xx-status. */
    HAPPY_PATH(200),
    /** Een verplicht veld of verplichte parameter ontbreekt. */
    MISSING_REQUIRED(400),
    /** Een veld heeft een waarde van het verkeerde datatype (bijv. een getal waar tekst hoort). */
    WRONG_TYPE(400),
    /** Een waarde zit precies op de grens (minimum/maximum/minLength/maxLength) -- moet slagen. */
    BOUNDARY_VALID(200),
    /** Een waarde zit net over de grens -- moet geweigerd worden. */
    BOUNDARY_INVALID(400),
    /** Een enum-veld krijgt een waarde die niet in de toegestane lijst staat. */
    INVALID_ENUM(400),
    /** Een veld met een formaat (uuid/email/date-time/date) krijgt een ongeldige waarde. */
    INVALID_FORMAT(400),
    /** Een beveiligd endpoint wordt aangeroepen zonder inloggegevens. */
    UNAUTHORIZED(401),
    /** Een beveiligd endpoint wordt aangeroepen met foutieve inloggegevens. */
    FORBIDDEN(403),
    /** Een niet-bestaande resource wordt opgevraagd (bijv. een willekeurig, onbestaand id). */
    NOT_FOUND(404);

    private final int defaultExpectedStatusCode;

    TestCaseType(int defaultExpectedStatusCode) {
        this.defaultExpectedStatusCode = defaultExpectedStatusCode;
    }

    public int defaultExpectedStatusCode() {
        return defaultExpectedStatusCode;
    }
}
