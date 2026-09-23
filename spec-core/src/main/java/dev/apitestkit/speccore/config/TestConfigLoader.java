package dev.apitestkit.speccore.config;

import dev.apitestkit.speccore.model.SpecModel;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Leest test-config.yaml in en valideert onbekende operationIds en sleutels streng:
 * elke afwijking geeft een foutmelding met bestandsnaam + regelnummer, in plaats van
 * de instelling stilzwijgend te negeren.
 */
public class TestConfigLoader {

    private static final Set<String> TOP_LEVEL_KEYS = Set.of("baseUrl", "auth", "operations", "caseTypes");
    private static final Set<String> AUTH_KEYS = Set.of("type", "tokenEnv", "usernameEnv", "passwordEnv", "headerName", "valueEnv");
    private static final Set<String> OPERATION_KEYS = Set.of("skip", "expectedStatusOverrides", "fixedTestData", "setupDependsOn");

    public TestConfig load(Path configPath, SpecModel spec) {
        if (!Files.exists(configPath)) {
            return TestConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Node root = new Yaml().compose(reader);
            if (root == null) {
                return TestConfig.defaults();
            }
            if (!(root instanceof MappingNode rootMapping)) {
                throw error(configPath, root, "test-config.yaml moet een YAML-mapping op het hoogste niveau bevatten");
            }
            return buildConfig(configPath, rootMapping, spec);
        } catch (IOException e) {
            throw new ConfigValidationException("Kon test-config.yaml niet lezen: " + configPath.toAbsolutePath() + " (" + e.getMessage() + ")");
        }
    }

    private TestConfig buildConfig(Path configPath, MappingNode root, SpecModel spec) {
        String baseUrl = null;
        AuthConfig auth = null;
        Map<String, OperationOverride> operationOverrides = new LinkedHashMap<>();
        Map<String, Boolean> caseTypesEnabled = new LinkedHashMap<>();

        for (NodeTuple tuple : root.getValue()) {
            String key = scalarValue(tuple.getKeyNode());
            if (!TOP_LEVEL_KEYS.contains(key)) {
                throw error(configPath, tuple.getKeyNode(), "Onbekende sleutel '" + key + "' in test-config.yaml");
            }
            switch (key) {
                case "baseUrl" -> baseUrl = scalarValue(tuple.getValueNode());
                case "auth" -> auth = buildAuth(configPath, requireMapping(configPath, tuple.getValueNode(), "auth"));
                case "operations" -> operationOverrides.putAll(
                        buildOperationOverrides(configPath, requireMapping(configPath, tuple.getValueNode(), "operations"), spec));
                case "caseTypes" -> caseTypesEnabled.putAll(buildCaseTypes(requireMapping(configPath, tuple.getValueNode(), "caseTypes")));
                default -> throw new IllegalStateException("onbereikbaar: " + key);
            }
        }

        return new TestConfig(baseUrl, auth, operationOverrides, caseTypesEnabled);
    }

    private AuthConfig buildAuth(Path configPath, MappingNode authNode) {
        Map<String, String> settings = new LinkedHashMap<>();
        String type = null;
        for (NodeTuple tuple : authNode.getValue()) {
            String key = scalarValue(tuple.getKeyNode());
            if (!AUTH_KEYS.contains(key)) {
                throw error(configPath, tuple.getKeyNode(), "Onbekende auth-sleutel '" + key + "' in test-config.yaml");
            }
            String value = scalarValue(tuple.getValueNode());
            if ("type".equals(key)) {
                type = value;
            } else {
                settings.put(key, value);
            }
        }
        return new AuthConfig(type, settings);
    }

    private Map<String, OperationOverride> buildOperationOverrides(Path configPath, MappingNode operationsNode, SpecModel spec) {
        Map<String, OperationOverride> overrides = new LinkedHashMap<>();
        for (NodeTuple tuple : operationsNode.getValue()) {
            String operationId = scalarValue(tuple.getKeyNode());
            if (spec.operationById(operationId).isEmpty()) {
                throw error(configPath, tuple.getKeyNode(),
                        "Onbekende operationId '" + operationId + "' in test-config.yaml -- deze komt niet voor in de OpenAPI-spec");
            }
            overrides.put(operationId, buildOperationOverride(configPath, requireMapping(configPath, tuple.getValueNode(), operationId)));
        }
        return overrides;
    }

    private OperationOverride buildOperationOverride(Path configPath, MappingNode node) {
        boolean skip = false;
        Map<String, Integer> statusOverrides = new LinkedHashMap<>();
        Map<String, Object> fixedTestData = new LinkedHashMap<>();
        String setupDependsOn = null;

        for (NodeTuple tuple : node.getValue()) {
            String key = scalarValue(tuple.getKeyNode());
            if (!OPERATION_KEYS.contains(key)) {
                throw error(configPath, tuple.getKeyNode(), "Onbekende sleutel '" + key + "' bij een operation in test-config.yaml");
            }
            switch (key) {
                case "skip" -> skip = Boolean.parseBoolean(scalarValue(tuple.getValueNode()));
                case "setupDependsOn" -> setupDependsOn = scalarValue(tuple.getValueNode());
                case "expectedStatusOverrides" -> {
                    MappingNode overridesNode = requireMapping(configPath, tuple.getValueNode(), "expectedStatusOverrides");
                    for (NodeTuple entry : overridesNode.getValue()) {
                        statusOverrides.put(scalarValue(entry.getKeyNode()), Integer.parseInt(scalarValue(entry.getValueNode())));
                    }
                }
                case "fixedTestData" -> {
                    MappingNode dataNode = requireMapping(configPath, tuple.getValueNode(), "fixedTestData");
                    for (NodeTuple entry : dataNode.getValue()) {
                        fixedTestData.put(scalarValue(entry.getKeyNode()), scalarValue(entry.getValueNode()));
                    }
                }
                default -> throw new IllegalStateException("onbereikbaar: " + key);
            }
        }

        return new OperationOverride(skip, statusOverrides, fixedTestData, setupDependsOn);
    }

    private Map<String, Boolean> buildCaseTypes(MappingNode node) {
        Map<String, Boolean> caseTypes = new LinkedHashMap<>();
        for (NodeTuple tuple : node.getValue()) {
            caseTypes.put(scalarValue(tuple.getKeyNode()), Boolean.parseBoolean(scalarValue(tuple.getValueNode())));
        }
        return caseTypes;
    }

    private MappingNode requireMapping(Path configPath, Node node, String context) {
        if (!(node instanceof MappingNode mappingNode)) {
            throw error(configPath, node, "'" + context + "' moet een YAML-mapping zijn in test-config.yaml");
        }
        return mappingNode;
    }

    private String scalarValue(Node node) {
        if (node instanceof ScalarNode scalarNode) {
            return scalarNode.getValue();
        }
        throw new ConfigValidationException("Verwachtte een enkelvoudige waarde op regel " + (node.getStartMark().getLine() + 1));
    }

    private ConfigValidationException error(Path configPath, Node node, String message) {
        int line = node.getStartMark().getLine() + 1;
        return new ConfigValidationException(message + " (" + configPath + ", regel " + line + ")");
    }
}
