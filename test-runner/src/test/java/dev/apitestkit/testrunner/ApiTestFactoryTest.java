package dev.apitestkit.testrunner;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Bewijst dat ApiTestFactory zelf -- de @TestFactory die spec-pad en test-config.yaml via
 * systeemproperties oppikt en DynamicTests genereert -- daadwerkelijk werkt, los van de
 * afzonderlijk geteste generator- en executor-onderdelen.
 */
class ApiTestFactoryTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("openapi.spec");
        System.clearProperty("testconfig.path");
        System.clearProperty("api.baseUrl");
    }

    @Test
    void apiTestsGenereertEnVoertDynamicTestsUitTegenCorrecteImplementatie() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(get(urlPathMatching("/pets")).atPriority(5)
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", absent())
                    .willReturn(aResponse().withStatus(401)));
            server.stubFor(post(urlEqualTo("/pets")).withHeader("X-API-Key", matching(".*"))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"teststringtes","status":"available"}""")));
            server.stubFor(get(urlEqualTo("/pets/00000000-0000-0000-0000-000000000000")).atPriority(1)
                    .willReturn(aResponse().withStatus(404)));
            server.stubFor(get(urlPathMatching("/pets/.*")).atPriority(5)
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"teststringtes","status":"available"}""")));

            System.setProperty("openapi.spec", "src/test/resources/fixtures/sample-api.yaml");
            System.setProperty("testconfig.path", "src/test/resources/fixtures/config/apikey-config.yaml");
            System.setProperty("api.baseUrl", server.baseUrl());

            List<DynamicTest> dynamicTests = new ApiTestFactory().apiTests().collect(Collectors.toList());
            assertThat(dynamicTests).hasSize(20);

            List<DynamicTest> runnable = dynamicTests.stream()
                    .filter(dt -> dt.getDisplayName().contains("HAPPY_PATH")
                            || dt.getDisplayName().contains("BOUNDARY_VALID")
                            || dt.getDisplayName().contains("UNAUTHORIZED")
                            || dt.getDisplayName().contains("NOT_FOUND"))
                    .toList();
            assertThat(runnable).isNotEmpty();
            for (DynamicTest dynamicTest : runnable) {
                assertThatCode(() -> dynamicTest.getExecutable().execute()).doesNotThrowAnyException();
            }
        } finally {
            server.stop();
        }
    }
}
