package dev.apitestkit.examples.adhoc;

import dev.apitestkit.testrunner.ApiTestFactory;

/**
 * Lege instapklasse om een eigen OpenAPI-spec te testen zonder zelf Java-code te hoeven schrijven.
 * Geef het pad naar je spec mee via -Dopenapi.spec en het adres van je draaiende API via
 * -Dapi.baseUrl (zie docs/UITLEG.md, hoofdstuk "Snelstartchecklist: een nieuwe eigen spec testen").
 *
 * <p>Draait bewust NIET automatisch mee in `mvn test`/`mvn verify` (zie de surefire-excludes in
 * examples/pom.xml) omdat er zonder -Dopenapi.spec niets zinnigs te testen valt. Start deze klasse
 * zelf, via IntelliJ of via `mvn test -Dtest=AdHocApiTest -Dopenapi.spec=... -Dapi.baseUrl=...`.
 */
class AdHocApiTest extends ApiTestFactory {
}
