package dev.apitestkit.speccore.config;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.Map;

public record OperationOverride(
        boolean skip,
        Map<String, Integer> expectedStatusOverrides,
        Map<String, Object> fixedTestData,
        String setupDependsOn
) {
    public OperationOverride {
        expectedStatusOverrides = OrderedMaps.copyOf(expectedStatusOverrides);
        fixedTestData = OrderedMaps.copyOf(fixedTestData);
    }

    public static OperationOverride empty() {
        return new OperationOverride(false, Map.of(), Map.of(), null);
    }
}
