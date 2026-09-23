package generated;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.config.TestConfigLoader;
import dev.apitestkit.speccore.generate.AuthMode;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseType;
import dev.apitestkit.speccore.model.SpecModel;
import dev.apitestkit.testrunner.TestCaseExecutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * GEGENEREERD door codegen (-Pgenerate) op basis van openapi.yaml.
 * Veilig om aan te passen: voeg eigen tests toe ONDER de "EINDE GEGENEREERD BLOK"-marker.
 * Alles daaronder blijft bij een volgende generatie behouden; het blok zelf wordt overschreven.
 */
class PetsGeneratedTest {

    private static TestCaseExecutor executor;

    @BeforeAll
    static void setUpFramework() {
        Path specPath = Path.of("openapi.yaml");
        SpecModel spec = new OpenApiSpecLoader().load(specPath);
        TestConfig config = new TestConfigLoader().load(specPath.resolveSibling("test-config.yaml"), spec);
        executor = new TestCaseExecutor(spec, config, specPath.toUri().toString());
    }

    // >>> GEGENEREERD -- niet handmatig bewerken, wordt overschreven bij opnieuw genereren

    // Geldige aanvraag conform spec
    @Test
    void listPets_happy_path() {
        executor.execute(new TestCase(
                "listPets",
                "GET",
                "/pets",
                TestCaseType.HAPPY_PATH,
                "Geldige aanvraag conform spec",
                Map.of(),
                Map.of("limit", 1),
                null,
                AuthMode.NONE,
                200,
                null));
    }

    // Grenswaarde: net op minimum voor limit
    @Test
    void listPets_boundary_valid() {
        executor.execute(new TestCase(
                "listPets",
                "GET",
                "/pets",
                TestCaseType.BOUNDARY_VALID,
                "Grenswaarde: net op minimum voor limit",
                Map.of(),
                Map.of("limit", 1),
                null,
                AuthMode.NONE,
                200,
                null));
    }

    // Grenswaarde: net op maximum voor limit
    @Test
    void listPets_boundary_valid_2() {
        executor.execute(new TestCase(
                "listPets",
                "GET",
                "/pets",
                TestCaseType.BOUNDARY_VALID,
                "Grenswaarde: net op maximum voor limit",
                Map.of(),
                Map.of("limit", 100),
                null,
                AuthMode.NONE,
                200,
                null));
    }

    // Geldige aanvraag conform spec
    @Test
    void createPet_happy_path() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.HAPPY_PATH,
                "Geldige aanvraag conform spec",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                201,
                null));
    }

    // Ontbrekend verplicht veld: status
    @Test
    void createPet_missing_required() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.MISSING_REQUIRED,
                "Ontbrekend verplicht veld: status",
                Map.of(),
                Map.of(),
                Map.of("name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Verkeerd datatype voor status
    @Test
    void createPet_wrong_type() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.WRONG_TYPE,
                "Verkeerd datatype voor status",
                Map.of(),
                Map.of(),
                Map.of("status", 1234567, "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Ongeldige enum-waarde voor status
    @Test
    void createPet_invalid_enum() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.INVALID_ENUM,
                "Ongeldige enum-waarde voor status",
                Map.of(),
                Map.of(),
                Map.of("status", "__ONGELDIGE_ENUM_WAARDE__", "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Ontbrekend verplicht veld: name
    @Test
    void createPet_missing_required_2() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.MISSING_REQUIRED,
                "Ontbrekend verplicht veld: name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Verkeerd datatype voor name
    @Test
    void createPet_wrong_type_2() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.WRONG_TYPE,
                "Verkeerd datatype voor name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", 1234567, "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Grenswaarde: net op minimum voor name
    @Test
    void createPet_boundary_valid() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.BOUNDARY_VALID,
                "Grenswaarde: net op minimum voor name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "te", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                201,
                null));
    }

    // Grenswaarde: net onder minimum voor name
    @Test
    void createPet_boundary_invalid() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.BOUNDARY_INVALID,
                "Grenswaarde: net onder minimum voor name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "t", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Grenswaarde: net op maximum voor name
    @Test
    void createPet_boundary_valid_2() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.BOUNDARY_VALID,
                "Grenswaarde: net op maximum voor name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "teststringteststringteststringteststringteststring", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                201,
                null));
    }

    // Grenswaarde: net boven maximum voor name
    @Test
    void createPet_boundary_invalid_2() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.BOUNDARY_INVALID,
                "Grenswaarde: net boven maximum voor name",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "teststringteststringteststringteststringteststringt", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Verkeerd datatype voor email
    @Test
    void createPet_wrong_type_3() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.WRONG_TYPE,
                "Verkeerd datatype voor email",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", 1234567, "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Ongeldig formaat voor email
    @Test
    void createPet_invalid_format() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.INVALID_FORMAT,
                "Ongeldig formaat voor email",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", "niet-een-emailadres", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.VALID,
                400,
                null));
    }

    // Verkeerd datatype voor createdAt
    @Test
    void createPet_wrong_type_4() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.WRONG_TYPE,
                "Verkeerd datatype voor createdAt",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", 1234567),
                AuthMode.VALID,
                400,
                null));
    }

    // Ongeldig formaat voor createdAt
    @Test
    void createPet_invalid_format_2() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.INVALID_FORMAT,
                "Ongeldig formaat voor createdAt",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "niet-een-datumtijd"),
                AuthMode.VALID,
                400,
                null));
    }

    // Aanvraag zonder credentials
    @Test
    void createPet_unauthorized() {
        executor.execute(new TestCase(
                "createPet",
                "POST",
                "/pets",
                TestCaseType.UNAUTHORIZED,
                "Aanvraag zonder credentials",
                Map.of(),
                Map.of(),
                Map.of("status", "available", "name", "Voorbeeldhuisdier", "email", "gebruiker@voorbeeld.nl", "createdAt", "2026-09-23T20:33:05.288520899Z"),
                AuthMode.NONE,
                401,
                null));
    }

    // Geldige aanvraag conform spec
    @Test
    void getPetById_happy_path() {
        executor.execute(new TestCase(
                "getPetById",
                "GET",
                "/pets/{petId}",
                TestCaseType.HAPPY_PATH,
                "Geldige aanvraag conform spec",
                Map.of("petId", "{{fromSetup}}"),
                Map.of(),
                null,
                AuthMode.NONE,
                200,
                "createPet"));
    }

    // Niet-bestaande resource
    @Test
    void getPetById_not_found() {
        executor.execute(new TestCase(
                "getPetById",
                "GET",
                "/pets/{petId}",
                TestCaseType.NOT_FOUND,
                "Niet-bestaande resource",
                Map.of("petId", "00000000-0000-0000-0000-000000000000"),
                Map.of(),
                null,
                AuthMode.VALID,
                404,
                null));
    }

    // <<< EINDE GEGENEREERD BLOK

    // Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.

}
