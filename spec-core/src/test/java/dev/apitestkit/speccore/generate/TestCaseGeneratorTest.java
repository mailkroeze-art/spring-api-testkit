package dev.apitestkit.speccore.generate;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.AuthConfig;
import dev.apitestkit.speccore.config.OperationOverride;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseGeneratorTest {

    private static final Path FIXTURE = Path.of("src/test/resources/fixtures/sample-api.yaml");
    private final TestCaseGenerator generator = new TestCaseGenerator();

    private SpecModel spec() {
        return new OpenApiSpecLoader().load(FIXTURE);
    }

    private List<TestCase> casesFor(String operationId, TestConfig config) {
        return generator.generate(spec(), config).stream()
                .filter(c -> c.operationId().equals(operationId))
                .toList();
    }

    @Test
    void genereertVerwachtAantalCasesPerOperatieMetStandaardConfig() {
        List<TestCase> all = generator.generate(spec(), TestConfig.defaults());

        Map<String, Long> perOperation = all.stream()
                .collect(Collectors.groupingBy(TestCase::operationId, Collectors.counting()));

        assertThat(perOperation).containsEntry("listPets", 3L);
        assertThat(perOperation).containsEntry("createPet", 15L);
        assertThat(perOperation).containsEntry("getPetById", 2L);
    }

    @Test
    void happyPathGebruiktGeldigeAuthAlsOperatieBeveiligdIs() {
        TestCase happyPath = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.HAPPY_PATH).findFirst().orElseThrow();

        assertThat(happyPath.authMode()).isEqualTo(AuthMode.VALID);
        assertThat(happyPath.expectedStatusCode()).isEqualTo(201);
    }

    @Test
    void happyPathGebruiktGeenAuthAlsOperatieNietBeveiligdIs() {
        TestCase happyPath = casesFor("listPets", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.HAPPY_PATH).findFirst().orElseThrow();

        assertThat(happyPath.authMode()).isEqualTo(AuthMode.NONE);
    }

    @Test
    void missingRequiredGenereertEenCasePerVerplichtVeld() {
        List<TestCase> missingRequired = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.MISSING_REQUIRED).toList();

        assertThat(missingRequired).hasSize(2);
        for (TestCase testCase : missingRequired) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) testCase.requestBody();
            assertThat(body.keySet()).hasSize(3);
            assertThat(testCase.expectedStatusCode()).isEqualTo(400);
        }
    }

    @Test
    void boundaryGenereertVierCasesVoorNameVeldMetMinEnMaxLength() {
        List<TestCase> boundaryCases = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.BOUNDARY_VALID || c.type() == TestCaseType.BOUNDARY_INVALID)
                .toList();

        assertThat(boundaryCases).hasSize(4);
        long validCount = boundaryCases.stream().filter(c -> c.type() == TestCaseType.BOUNDARY_VALID).count();
        long invalidCount = boundaryCases.stream().filter(c -> c.type() == TestCaseType.BOUNDARY_INVALID).count();
        assertThat(validCount).isEqualTo(2);
        assertThat(invalidCount).isEqualTo(2);
        boundaryCases.forEach(c -> assertThat(c.expectedStatusCode()).isIn(201, 400));
    }

    @Test
    void invalidEnumGenereertCaseVoorStatusVeld() {
        List<TestCase> invalidEnumCases = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.INVALID_ENUM).toList();

        assertThat(invalidEnumCases).hasSize(1);
        assertThat(invalidEnumCases.get(0).expectedStatusCode()).isEqualTo(400);
    }

    @Test
    void invalidFormatGenereertCasesVoorEmailEnCreatedAt() {
        List<TestCase> invalidFormatCases = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.INVALID_FORMAT).toList();

        assertThat(invalidFormatCases).hasSize(2);
    }

    @Test
    void unauthorizedCaseAlleenAlsOperatieAuthVereistEn401Gedocumenteerd() {
        List<TestCase> unauthorized = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.UNAUTHORIZED).toList();

        assertThat(unauthorized).hasSize(1);
        assertThat(unauthorized.get(0).authMode()).isEqualTo(AuthMode.NONE);
    }

    @Test
    void geenForbiddenCaseAlsStatus403NietGedocumenteerdIs() {
        List<TestCase> forbidden = casesFor("createPet", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.FORBIDDEN).toList();

        assertThat(forbidden).isEmpty();
    }

    @Test
    void notFoundGenereertCaseMetOngeldigPathParameter() {
        List<TestCase> notFoundCases = casesFor("getPetById", TestConfig.defaults()).stream()
                .filter(c -> c.type() == TestCaseType.NOT_FOUND).toList();

        assertThat(notFoundCases).hasSize(1);
        assertThat(notFoundCases.get(0).expectedStatusCode()).isEqualTo(404);
        assertThat(notFoundCases.get(0).pathParams()).containsKey("petId");
    }

    @Test
    void geenErrorCasesAlsStatus400NietGedocumenteerdIsVoorGetPetById() {
        List<TestCase> cases = casesFor("getPetById", TestConfig.defaults());

        assertThat(cases).noneMatch(c -> c.type() == TestCaseType.WRONG_TYPE
                || c.type() == TestCaseType.MISSING_REQUIRED
                || c.type() == TestCaseType.BOUNDARY_INVALID);
    }

    @Test
    void overrideSkipSlaatOperatieVolledigOver() {
        TestConfig config = new TestConfig(null, null,
                Map.of("createPet", new OperationOverride(true, Map.of(), Map.of(), null)), Map.of());

        assertThat(casesFor("createPet", config)).isEmpty();
    }

    @Test
    void overrideExpectedStatusWordtToegepastOpHappyPath() {
        TestConfig config = new TestConfig(null, null,
                Map.of("createPet", new OperationOverride(false, Map.of("HAPPY_PATH", 200), Map.of(), null)), Map.of());

        TestCase happyPath = casesFor("createPet", config).stream()
                .filter(c -> c.type() == TestCaseType.HAPPY_PATH).findFirst().orElseThrow();

        assertThat(happyPath.expectedStatusCode()).isEqualTo(200);
    }

    @Test
    void overrideFixedTestDataOverschrijftGegenereerdeWaarde() {
        TestConfig config = new TestConfig(null, null,
                Map.of("createPet", new OperationOverride(false, Map.of(), Map.of("name", "VasteTestNaam"), null)), Map.of());

        TestCase happyPath = casesFor("createPet", config).stream()
                .filter(c -> c.type() == TestCaseType.HAPPY_PATH).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) happyPath.requestBody();
        assertThat(body.get("name")).isEqualTo("VasteTestNaam");
    }

    @Test
    void overrideCaseTypeUitgezetGeenCasesVanDatType() {
        TestConfig config = new TestConfig(null, null, Map.of(), Map.of("INVALID_ENUM", false));

        assertThat(casesFor("createPet", config)).noneMatch(c -> c.type() == TestCaseType.INVALID_ENUM);
    }

    @Test
    void setupDependsOnZetPlaceholderVoorHappyPathMaarNietVoorNotFound() {
        TestConfig config = new TestConfig(null, null,
                Map.of("getPetById", new OperationOverride(false, Map.of(), Map.of(), "createPet")), Map.of());

        TestCase happyPath = casesFor("getPetById", config).stream()
                .filter(c -> c.type() == TestCaseType.HAPPY_PATH).findFirst().orElseThrow();
        TestCase notFound = casesFor("getPetById", config).stream()
                .filter(c -> c.type() == TestCaseType.NOT_FOUND).findFirst().orElseThrow();

        assertThat(happyPath.pathParams().get("petId")).isEqualTo(TestCase.SETUP_PLACEHOLDER);
        assertThat(happyPath.setupOperationId()).isEqualTo("createPet");
        assertThat(notFound.setupOperationId()).isNull();
    }
}
