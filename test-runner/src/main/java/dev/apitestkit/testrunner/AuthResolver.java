package dev.apitestkit.testrunner;

import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.generate.AuthMode;
import io.restassured.specification.RequestSpecification;

/**
 * Voegt authenticatie toe aan een REST Assured-request op basis van AuthMode en de auth-instellingen
 * uit test-config.yaml. Geheime waarden komen altijd uit env vars, nooit uit het config-bestand zelf.
 */
public class AuthResolver {

    public void apply(RequestSpecification request, AuthMode mode, TestConfig config) {
        if (mode == AuthMode.NONE || config.auth() == null || config.auth().type() == null) {
            return;
        }
        if (mode == AuthMode.VALID) {
            applyValid(request, config);
        } else if (mode == AuthMode.INVALID) {
            applyInvalid(request, config);
        }
    }

    private void applyValid(RequestSpecification request, TestConfig config) {
        switch (config.auth().type()) {
            case "bearer" -> request.header("Authorization", "Bearer " + config.resolveAuthSecret("tokenEnv").orElse(""));
            case "basic" -> request.auth().preemptive().basic(
                    config.resolveAuthSecret("usernameEnv").orElse(""),
                    config.resolveAuthSecret("passwordEnv").orElse(""));
            case "apiKey" -> request.header(headerName(config), config.resolveAuthSecret("valueEnv").orElse(""));
            default -> throw new IllegalStateException("Onbekend auth-type in test-config.yaml: " + config.auth().type());
        }
    }

    private void applyInvalid(RequestSpecification request, TestConfig config) {
        switch (config.auth().type()) {
            case "bearer" -> request.header("Authorization", "Bearer ongeldig-token");
            case "basic" -> request.auth().preemptive().basic("ongeldige-gebruiker", "ongeldig-wachtwoord");
            case "apiKey" -> request.header(headerName(config), "ongeldige-sleutel");
            default -> throw new IllegalStateException("Onbekend auth-type in test-config.yaml: " + config.auth().type());
        }
    }

    private String headerName(TestConfig config) {
        return config.auth().settings().getOrDefault("headerName", "X-API-Key");
    }
}
