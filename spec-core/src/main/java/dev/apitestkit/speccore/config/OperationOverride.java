package dev.apitestkit.speccore.config;

import java.util.Map;

public record OperationOverride(
        boolean skip,
        Map<String, Integer> expectedStatusOverrides,
        Map<String, Object> fixedTestData,
        String setupDependsOn
) {
    public OperationOverride {
        expectedStatusOverrides = expectedStatusOverrides == null ? Map.of() : Map.copyOf(expectedStatusOverrides);
        fixedTestData = fixedTestData == null ? Map.of() : Map.copyOf(fixedTestData);
    }

    public static OperationOverride empty() {
        return new OperationOverride(false, Map.of(), Map.of(), null);
    }
}
