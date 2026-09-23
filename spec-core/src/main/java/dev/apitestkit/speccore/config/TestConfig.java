package dev.apitestkit.speccore.config;

import java.util.Map;
import java.util.Optional;

public record TestConfig(
        String baseUrl,
        AuthConfig auth,
        Map<String, OperationOverride> operationOverrides,
        Map<String, Boolean> caseTypesEnabled
) {
    public TestConfig {
        operationOverrides = operationOverrides == null ? Map.of() : Map.copyOf(operationOverrides);
        caseTypesEnabled = caseTypesEnabled == null ? Map.of() : Map.copyOf(caseTypesEnabled);
    }

    public static TestConfig defaults() {
        return new TestConfig(null, null, Map.of(), Map.of());
    }

    public OperationOverride overrideFor(String operationId) {
        return operationOverrides.getOrDefault(operationId, OperationOverride.empty());
    }

    public boolean isCaseTypeEnabled(String caseType) {
        return caseTypesEnabled.getOrDefault(caseType, Boolean.TRUE);
    }

    /**
     * baseUrl uit dit bestand kan altijd overschreven worden via de omgevingsvariabele
     * API_BASE_URL of de systeemproperty -Dapi.baseUrl.
     */
    public String resolveBaseUrl() {
        return resolveBaseUrl(System.getProperty("api.baseUrl"), System.getenv("API_BASE_URL"));
    }

    String resolveBaseUrl(String systemProperty, String envVar) {
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        if (envVar != null && !envVar.isBlank()) {
            return envVar;
        }
        return baseUrl;
    }

    public Optional<String> resolveAuthSecret(String envVarKey) {
        if (auth == null) {
            return Optional.empty();
        }
        String envVarName = auth.settings().get(envVarKey);
        if (envVarName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(System.getenv(envVarName));
    }
}
