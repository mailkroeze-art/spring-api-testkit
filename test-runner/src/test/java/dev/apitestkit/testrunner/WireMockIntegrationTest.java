package dev.apitestkit.testrunner;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.AuthConfig;
import dev.apitestkit.speccore.config.OperationOverride;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.generate.TestCaseType;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Draait de gegenereerde tests tegen een WireMock-stub die de voorbeeldspec correct implementeert
 * (alles groen), en tegen bewust foutieve implementaties (rood), zoals Fase 3 van de opdracht vereist.
 */
class WireMockIntegrationTest {

    private static final Path SPEC_PATH = Path.of("src/test/resources/fixtures/sample-api.yaml");

    private SpecModel spec() {
        return new OpenApiSpecLoader().load(SPEC_PATH);
    }

    private TestConfig configWithBaseUrl(WireMockServer server) {
        AuthConfig auth = new AuthConfig("apiKey", Map.of("headerName", "X-API-Key", "valueEnv", "TESTKIT_API_KEY"));
        return new TestConfig(server.baseUrl(), auth, Map.of(), Map.of());
    }

    private TestCaseExecutor executorFor(WireMockServer server, TestConfig config) {
        return new TestCaseExecutor(spec(), config, SPEC_PATH.toUri().toString());
    }

    @Test
    void correcteImplementatieLaatHappyPathBoundaryEnNotFoundCasesSlagen() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(get(urlPathMatching("/pets")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("[]")));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", absent())
                    .willReturn(aResponse().withStatus(401)));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", matching(".*"))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"teststringtes","status":"available"}""")));
            server.stubFor(get(urlEqualTo("/pets/00000000-0000-0000-0000-000000000000")).atPriority(1)
                    .willReturn(aResponse().withStatus(404)));
            server.stubFor(get(urlPathMatching("/pets/.*")).atPriority(5).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"teststringtes","status":"available"}""")));

            TestConfig config = configWithBaseUrl(server);
            TestCaseExecutor executor = executorFor(server, config);
            List<TestCase> cases = new TestCaseGenerator().generate(spec(), config).stream()
                    .filter(c -> c.type() == TestCaseType.HAPPY_PATH
                            || c.type() == TestCaseType.BOUNDARY_VALID
                            || c.type() == TestCaseType.UNAUTHORIZED
                            || c.type() == TestCaseType.NOT_FOUND)
                    .toList();

            assertThat(cases).isNotEmpty();
            for (TestCase testCase : cases) {
                executor.execute(testCase);
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void ontbrekendeAutorisatiecontroleMaaktUnauthorizedCaseRood() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(post(urlEqualTo("/pets")).willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"teststringtes","status":"available"}""")));

            TestConfig config = configWithBaseUrl(server);
            TestCaseExecutor executor = executorFor(server, config);
            TestCase unauthorized = new TestCaseGenerator().generate(spec(), config).stream()
                    .filter(c -> c.operationId().equals("createPet") && c.type() == TestCaseType.UNAUTHORIZED)
                    .findFirst().orElseThrow();

            assertThatThrownBy(() -> executor.execute(unauthorized))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("createPet")
                    .hasMessageContaining("UNAUTHORIZED")
                    .hasMessageContaining("Verwachte statuscode: 401");
        } finally {
            server.stop();
        }
    }

    @Test
    void responseZonderVerplichtVeldMaaktCaseRoodDoorSchemaValidatie() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", matching(".*"))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}""")));

            TestConfig config = configWithBaseUrl(server);
            TestCaseExecutor executor = executorFor(server, config);
            TestCase happyPath = new TestCaseGenerator().generate(spec(), config).stream()
                    .filter(c -> c.operationId().equals("createPet") && c.type() == TestCaseType.HAPPY_PATH)
                    .findFirst().orElseThrow();

            assertThatThrownBy(() -> executor.execute(happyPath))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("createPet")
                    .hasMessageContaining("niet aan de OpenAPI-spec");
        } finally {
            server.stop();
        }
    }

    @Test
    void setupDependsOnHaaltEchtIdOpBijDeSetupOperatieVoordatDeCaseDraait() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            String createdId = "11111111-1111-1111-1111-111111111111";
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", matching(".*"))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":\"" + createdId + "\",\"name\":\"teststringtes\",\"status\":\"available\"}")));
            server.stubFor(get(urlEqualTo("/pets/" + createdId)).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"" + createdId + "\",\"name\":\"teststringtes\",\"status\":\"available\"}")));

            TestConfig config = new TestConfig(server.baseUrl(),
                    new AuthConfig("apiKey", Map.of("headerName", "X-API-Key", "valueEnv", "TESTKIT_API_KEY")),
                    Map.of("getPetById", new OperationOverride(false, Map.of(), Map.of(), "createPet")),
                    Map.of());
            TestCaseExecutor executor = executorFor(server, config);
            TestCase happyPath = new TestCaseGenerator().generate(spec(), config).stream()
                    .filter(c -> c.operationId().equals("getPetById") && c.type() == TestCaseType.HAPPY_PATH)
                    .findFirst().orElseThrow();

            executor.execute(happyPath);

            server.verify(getRequestedFor(urlEqualTo("/pets/" + createdId)));
        } finally {
            server.stop();
        }
    }
}
