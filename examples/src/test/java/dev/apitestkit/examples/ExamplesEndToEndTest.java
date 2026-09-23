package dev.apitestkit.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.config.TestConfigLoader;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.generate.TestCaseType;
import dev.apitestkit.speccore.model.SpecModel;
import dev.apitestkit.testrunner.TestCaseExecutor;
import org.junit.jupiter.api.AfterEach;
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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Draait de daadwerkelijke examples/openapi.yaml en examples/test-config.yaml (dezelfde bestanden
 * als aangehaald in docs/UITLEG.md) tegen een correcte WireMock-implementatie. Bewijst dat
 * fixedTestData en setupDependsOn ook in de echte voorbeeldconfiguratie werken zoals gedocumenteerd.
 */
class ExamplesEndToEndTest {

    private static final Path SPEC_PATH = Path.of("openapi.yaml");
    private static final Path CONFIG_PATH = Path.of("test-config.yaml");

    @AfterEach
    void clearBaseUrlOverride() {
        System.clearProperty("api.baseUrl");
    }

    @Test
    void examplesConfigGenereertEnDraaitSuccesvolTegenCorrecteImplementatie() {
        SpecModel spec = new OpenApiSpecLoader().load(SPEC_PATH);
        TestConfig config = new TestConfigLoader().load(CONFIG_PATH, spec);

        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        System.setProperty("api.baseUrl", server.baseUrl());
        try {
            String createdId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
            server.stubFor(get(urlPathMatching("/pets")).atPriority(5)
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", absent())
                    .willReturn(aResponse().withStatus(401)));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", matching(".*"))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":\"" + createdId + "\",\"name\":\"Voorbeeldhuisdier\",\"status\":\"available\"}")));
            server.stubFor(get(urlEqualTo("/pets/00000000-0000-0000-0000-000000000000")).atPriority(1)
                    .willReturn(aResponse().withStatus(404)));
            server.stubFor(get(urlEqualTo("/pets/" + createdId)).atPriority(1)
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":\"" + createdId + "\",\"name\":\"Voorbeeldhuisdier\",\"status\":\"available\"}")));

            TestCaseExecutor executor = new TestCaseExecutor(spec, config, SPEC_PATH.toUri().toString());
            List<TestCase> cases = new TestCaseGenerator().generate(spec, config);

            TestCase createPetHappy = findCase(cases, "createPet", TestCaseType.HAPPY_PATH);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) createPetHappy.requestBody();
            assertThat(body).containsEntry("name", "Voorbeeldhuisdier");

            List<TestCase> toRun = cases.stream()
                    .filter(c -> c.type() == TestCaseType.HAPPY_PATH
                            || c.type() == TestCaseType.BOUNDARY_VALID
                            || c.type() == TestCaseType.UNAUTHORIZED
                            || c.type() == TestCaseType.NOT_FOUND)
                    .toList();
            assertThat(toRun).isNotEmpty();
            for (TestCase testCase : toRun) {
                executor.execute(testCase);
            }

            server.verify(getRequestedFor(urlEqualTo("/pets/" + createdId)));
        } finally {
            server.stop();
        }
    }

    private TestCase findCase(List<TestCase> cases, String operationId, TestCaseType type) {
        return cases.stream().filter(c -> c.operationId().equals(operationId) && c.type() == type)
                .findFirst().orElseThrow();
    }
}
