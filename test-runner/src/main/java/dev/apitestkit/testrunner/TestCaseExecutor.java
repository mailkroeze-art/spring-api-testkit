package dev.apitestkit.testrunner;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.generate.TestCaseType;
import dev.apitestkit.speccore.model.SpecModel;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Voert een TestCase daadwerkelijk uit tegen een draaiende API: lost eventuele setup-afhankelijkheid
 * op, stuurt het request, en valideert zowel de verwachte statuscode als (via de OpenApiValidationFilter)
 * dat de response-statuscode gedocumenteerd is en de body schema-valide is.
 */
public class TestCaseExecutor {

    private final SpecModel spec;
    private final TestConfig config;
    private final AuthResolver authResolver = new AuthResolver();
    private final TestCaseGenerator generator = new TestCaseGenerator();
    private final OpenApiValidationFilter validationFilter;

    public TestCaseExecutor(SpecModel spec, TestConfig config, String openApiSpecLocation) {
        this.spec = spec;
        this.config = config;
        this.validationFilter = new OpenApiValidationFilter(openApiSpecLocation);
    }

    public void execute(TestCase testCase) {
        TestCase resolved = resolveSetup(testCase);
        Response response;
        try {
            response = performRequest(resolved, true);
        } catch (RuntimeException e) {
            throw new AssertionError(TestFailureReporter.validationFailure(resolved, e.getMessage()), e);
        }
        if (response.statusCode() != resolved.expectedStatusCode()) {
            throw new AssertionError(TestFailureReporter.statusMismatch(resolved, response));
        }
    }

    private TestCase resolveSetup(TestCase testCase) {
        if (!testCase.requiresSetup()) {
            return testCase;
        }
        String id = executeSetupAndExtractId(testCase.setupOperationId());
        Map<String, Object> pathParams = new LinkedHashMap<>(testCase.pathParams());
        pathParams.replaceAll((key, value) -> TestCase.SETUP_PLACEHOLDER.equals(value) ? id : value);
        return new TestCase(testCase.operationId(), testCase.httpMethod(), testCase.path(), testCase.type(),
                testCase.description(), pathParams, testCase.queryParams(), testCase.requestBody(),
                testCase.authMode(), testCase.expectedStatusCode(), null);
    }

    private String executeSetupAndExtractId(String setupOperationId) {
        TestCase setupCase = generator.generate(spec, config).stream()
                .filter(c -> c.operationId().equals(setupOperationId) && c.type() == TestCaseType.HAPPY_PATH)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Kan setup-operatie '" + setupOperationId + "' niet vinden of heeft geen HAPPY_PATH-case in de spec"));

        Response response = performRequest(setupCase, false);
        Object idValue = response.jsonPath().get("id");
        if (idValue == null) {
            throw new IllegalStateException(
                    "Setup-operatie '" + setupOperationId + "' gaf een response zonder 'id'-veld -- kan setupDependsOn niet oplossen");
        }
        return String.valueOf(idValue);
    }

    private Response performRequest(TestCase testCase, boolean validate) {
        String baseUrl = config.resolveBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Geen baseUrl geconfigureerd -- zet baseUrl in test-config.yaml of gebruik -Dapi.baseUrl / de omgevingsvariabele API_BASE_URL");
        }

        RequestSpecification request = RestAssured.given().baseUri(baseUrl);
        if (validate) {
            request.filter(validationFilter);
        }
        authResolver.apply(request, testCase.authMode(), config);
        testCase.pathParams().forEach(request::pathParam);
        testCase.queryParams().forEach(request::queryParam);
        if (testCase.requestBody() != null) {
            request.contentType("application/json").body(testCase.requestBody());
        }
        return request.request(testCase.httpMethod(), testCase.path());
    }
}
