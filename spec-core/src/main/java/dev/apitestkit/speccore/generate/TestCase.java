package dev.apitestkit.speccore.generate;

import java.util.Map;

public record TestCase(
        String operationId,
        String httpMethod,
        String path,
        TestCaseType type,
        String description,
        Map<String, Object> pathParams,
        Map<String, Object> queryParams,
        Object requestBody,
        AuthMode authMode,
        int expectedStatusCode,
        String setupOperationId
) {
    /**
     * Placeholder-waarde voor een pad-parameter die pas op runtime bekend is,
     * bijvoorbeeld een id die eerst via een setupOperationId-aanroep opgehaald moet worden.
     */
    public static final String SETUP_PLACEHOLDER = "{{fromSetup}}";

    public TestCase {
        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
        queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
    }

    public boolean requiresSetup() {
        return setupOperationId != null;
    }

    public String displayName() {
        return httpMethod + " " + path + " [" + type + "]";
    }
}
