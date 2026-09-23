package dev.apitestkit.speccore.generate;

import dev.apitestkit.speccore.config.OperationOverride;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.model.OperationModel;
import dev.apitestkit.speccore.model.ParameterModel;
import dev.apitestkit.speccore.model.SchemaModel;
import dev.apitestkit.speccore.model.SpecModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Bouwt de volledige lijst DynamicTest-testcases voor een OpenAPI-spec, rekening houdend met
 * de overrides in test-config.yaml. Een foutcase (MISSING_REQUIRED, WRONG_TYPE, BOUNDARY_INVALID,
 * INVALID_ENUM, INVALID_FORMAT) wordt alleen gegenereerd als de verwachte statuscode ook echt
 * gedocumenteerd is in de spec -- anders is er niets zinnigs om tegen te toetsen.
 */
public class TestCaseGenerator {

    private enum Location { BODY, QUERY }

    private record Field(String name, SchemaModel schema, boolean required, Location location) {
    }

    private final ExampleValueGenerator valueGenerator = new ExampleValueGenerator();

    public List<TestCase> generate(SpecModel spec, TestConfig config) {
        List<TestCase> cases = new ArrayList<>();
        for (OperationModel operation : spec.operations()) {
            OperationOverride override = config.overrideFor(operation.operationId());
            if (override.skip()) {
                continue;
            }
            cases.addAll(generateForOperation(operation, config, override));
        }
        return cases;
    }

    private List<TestCase> generateForOperation(OperationModel operation, TestConfig config, OperationOverride override) {
        List<TestCase> cases = new ArrayList<>();

        Integer happyStatus = firstDocumented2xx(operation);
        if (happyStatus == null) {
            return cases;
        }

        Map<String, Object> basePathParams = buildBasePathParams(operation, override);
        Map<String, Object> baseQueryParams = buildBaseQueryParams(operation, override);
        Object baseBody = buildBaseBody(operation, override);
        String setupOperationId = override.setupDependsOn();

        if (config.isCaseTypeEnabled(TestCaseType.HAPPY_PATH.name())) {
            cases.add(new TestCase(
                    operation.operationId(), operation.httpMethod(), operation.path(),
                    TestCaseType.HAPPY_PATH, "Geldige aanvraag conform spec",
                    basePathParams, baseQueryParams, baseBody,
                    operation.requiresAuthentication() ? AuthMode.VALID : AuthMode.NONE,
                    expectedFor(override, TestCaseType.HAPPY_PATH, happyStatus),
                    setupOperationId));
        }

        List<Field> fields = collectFields(operation);
        boolean status400Documented = operation.responses().containsKey("400");

        for (Field field : fields) {
            if (field.required() && config.isCaseTypeEnabled(TestCaseType.MISSING_REQUIRED.name()) && status400Documented) {
                cases.add(missingRequiredCase(operation, field, basePathParams, baseQueryParams, baseBody, override, setupOperationId));
            }
            if (field.schema().type() != null && config.isCaseTypeEnabled(TestCaseType.WRONG_TYPE.name()) && status400Documented) {
                Object invalid = valueGenerator.invalidTypeValue(field.schema());
                cases.add(fieldCase(operation, TestCaseType.WRONG_TYPE, "Verkeerd datatype voor " + field.name(),
                        field, invalid, basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.WRONG_TYPE, 400), setupOperationId));
            }
            addBoundaryCases(cases, operation, field, basePathParams, baseQueryParams, baseBody, config, override, happyStatus, status400Documented, setupOperationId);
            if (!field.schema().enumValues().isEmpty() && config.isCaseTypeEnabled(TestCaseType.INVALID_ENUM.name()) && status400Documented) {
                Object invalid = valueGenerator.invalidEnumValue(field.schema());
                cases.add(fieldCase(operation, TestCaseType.INVALID_ENUM, "Ongeldige enum-waarde voor " + field.name(),
                        field, invalid, basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.INVALID_ENUM, 400), setupOperationId));
            }
            if (isFormatted(field.schema()) && config.isCaseTypeEnabled(TestCaseType.INVALID_FORMAT.name()) && status400Documented) {
                Object invalid = valueGenerator.invalidFormatValue(field.schema());
                cases.add(fieldCase(operation, TestCaseType.INVALID_FORMAT, "Ongeldig formaat voor " + field.name(),
                        field, invalid, basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.INVALID_FORMAT, 400), setupOperationId));
            }
        }

        if (operation.requiresAuthentication()) {
            if (config.isCaseTypeEnabled(TestCaseType.UNAUTHORIZED.name()) && operation.responses().containsKey("401")) {
                cases.add(new TestCase(operation.operationId(), operation.httpMethod(), operation.path(),
                        TestCaseType.UNAUTHORIZED, "Aanvraag zonder credentials",
                        basePathParams, baseQueryParams, baseBody, AuthMode.NONE,
                        expectedFor(override, TestCaseType.UNAUTHORIZED, 401), setupOperationId));
            }
            if (config.isCaseTypeEnabled(TestCaseType.FORBIDDEN.name()) && operation.responses().containsKey("403")) {
                cases.add(new TestCase(operation.operationId(), operation.httpMethod(), operation.path(),
                        TestCaseType.FORBIDDEN, "Aanvraag met ongeldige credentials",
                        basePathParams, baseQueryParams, baseBody, AuthMode.INVALID,
                        expectedFor(override, TestCaseType.FORBIDDEN, 403), setupOperationId));
            }
        }

        if (config.isCaseTypeEnabled(TestCaseType.NOT_FOUND.name())
                && operation.responses().containsKey("404")
                && !basePathParams.isEmpty()) {
            Map<String, Object> notFoundPathParams = new LinkedHashMap<>(basePathParams);
            String firstKey = notFoundPathParams.keySet().iterator().next();
            notFoundPathParams.put(firstKey, "00000000-0000-0000-0000-000000000000");
            cases.add(new TestCase(operation.operationId(), operation.httpMethod(), operation.path(),
                    TestCaseType.NOT_FOUND, "Niet-bestaande resource",
                    notFoundPathParams, baseQueryParams, null, AuthMode.VALID,
                    expectedFor(override, TestCaseType.NOT_FOUND, 404), null));
        }

        return cases;
    }

    private void addBoundaryCases(List<TestCase> cases, OperationModel operation, Field field,
                                   Map<String, Object> basePathParams, Map<String, Object> baseQueryParams, Object baseBody,
                                   TestConfig config, OperationOverride override, int happyStatus, boolean status400Documented,
                                   String setupOperationId) {
        if (!config.isCaseTypeEnabled(TestCaseType.BOUNDARY_VALID.name()) && !config.isCaseTypeEnabled(TestCaseType.BOUNDARY_INVALID.name())) {
            return;
        }
        SchemaModel schema = field.schema();
        boolean hasMin = schema.minimum() != null || schema.minLength() != null;
        boolean hasMax = schema.maximum() != null || schema.maxLength() != null;

        if (hasMin) {
            if (config.isCaseTypeEnabled(TestCaseType.BOUNDARY_VALID.name())) {
                cases.add(fieldCase(operation, TestCaseType.BOUNDARY_VALID, "Grenswaarde: net op minimum voor " + field.name(),
                        field, valueGenerator.minBoundaryValue(schema), basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.BOUNDARY_VALID, happyStatus), setupOperationId));
            }
            if (config.isCaseTypeEnabled(TestCaseType.BOUNDARY_INVALID.name()) && status400Documented) {
                cases.add(fieldCase(operation, TestCaseType.BOUNDARY_INVALID, "Grenswaarde: net onder minimum voor " + field.name(),
                        field, valueGenerator.belowMinBoundaryValue(schema), basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.BOUNDARY_INVALID, 400), setupOperationId));
            }
        }
        if (hasMax) {
            if (config.isCaseTypeEnabled(TestCaseType.BOUNDARY_VALID.name())) {
                cases.add(fieldCase(operation, TestCaseType.BOUNDARY_VALID, "Grenswaarde: net op maximum voor " + field.name(),
                        field, valueGenerator.maxBoundaryValue(schema), basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.BOUNDARY_VALID, happyStatus), setupOperationId));
            }
            if (config.isCaseTypeEnabled(TestCaseType.BOUNDARY_INVALID.name()) && status400Documented) {
                cases.add(fieldCase(operation, TestCaseType.BOUNDARY_INVALID, "Grenswaarde: net boven maximum voor " + field.name(),
                        field, valueGenerator.aboveMaxBoundaryValue(schema), basePathParams, baseQueryParams, baseBody,
                        expectedFor(override, TestCaseType.BOUNDARY_INVALID, 400), setupOperationId));
            }
        }
    }

    private TestCase missingRequiredCase(OperationModel operation, Field field, Map<String, Object> basePathParams,
                                          Map<String, Object> baseQueryParams, Object baseBody, OperationOverride override,
                                          String setupOperationId) {
        Map<String, Object> queryParams = new LinkedHashMap<>(baseQueryParams);
        Object body = baseBody;
        if (field.location() == Location.QUERY) {
            queryParams.remove(field.name());
        } else {
            body = withoutKey(baseBody, field.name());
        }
        return new TestCase(operation.operationId(), operation.httpMethod(), operation.path(),
                TestCaseType.MISSING_REQUIRED, "Ontbrekend verplicht veld: " + field.name(),
                basePathParams, queryParams, body, AuthMode.VALID,
                expectedFor(override, TestCaseType.MISSING_REQUIRED, 400), setupOperationId);
    }

    private TestCase fieldCase(OperationModel operation, TestCaseType type, String description, Field field, Object invalidValue,
                                Map<String, Object> basePathParams, Map<String, Object> baseQueryParams, Object baseBody,
                                int expectedStatus, String setupOperationId) {
        Map<String, Object> queryParams = new LinkedHashMap<>(baseQueryParams);
        Object body = baseBody;
        if (field.location() == Location.QUERY) {
            queryParams.put(field.name(), invalidValue);
        } else {
            body = withReplacedKey(baseBody, field.name(), invalidValue);
        }
        return new TestCase(operation.operationId(), operation.httpMethod(), operation.path(),
                type, description, basePathParams, queryParams, body,
                operation.requiresAuthentication() ? AuthMode.VALID : AuthMode.NONE,
                expectedStatus, setupOperationId);
    }

    /**
     * Verzamelt alle body- en query-velden die kandidaat zijn voor WRONG_TYPE/BOUNDARY/INVALID_ENUM/
     * INVALID_FORMAT-tests. Pad-parameters (bijv. het id in "/pets/{petId}") doen hier bewust NIET
     * aan mee: het vervangen van een pad-parameter door een ongeldige waarde levert al een aparte,
     * betekenisvollere test op (NOT_FOUND), en verder fuzzen van het pad zou vooral ruis geven.
     */
    private List<Field> collectFields(OperationModel operation) {
        List<Field> fields = new ArrayList<>();
        operation.requestBody().ifPresent(requestBody -> {
            SchemaModel schema = requestBody.schema();
            if (schema != null) {
                schema.properties().forEach((name, propSchema) ->
                        fields.add(new Field(name, propSchema, schema.required().contains(name), Location.BODY)));
            }
        });
        operation.parameters().stream()
                .filter(p -> "query".equals(p.in()))
                .forEach(p -> fields.add(new Field(p.name(), p.schema(), p.required(), Location.QUERY)));
        return fields;
    }

    private Map<String, Object> buildBasePathParams(OperationModel operation, OperationOverride override) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (ParameterModel parameter : operation.parameters()) {
            if (!"path".equals(parameter.in())) {
                continue;
            }
            Object value = override.setupDependsOn() != null
                    ? TestCase.SETUP_PLACEHOLDER
                    : valueGenerator.validValue(parameter.schema());
            params.put(parameter.name(), value);
        }
        override.fixedTestData().forEach((key, value) -> {
            if (params.containsKey(key)) {
                params.put(key, value);
            }
        });
        return params;
    }

    private Map<String, Object> buildBaseQueryParams(OperationModel operation, OperationOverride override) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (ParameterModel parameter : operation.parameters()) {
            if (!"query".equals(parameter.in())) {
                continue;
            }
            params.put(parameter.name(), valueGenerator.validValue(parameter.schema()));
        }
        override.fixedTestData().forEach((key, value) -> {
            if (params.containsKey(key)) {
                params.put(key, value);
            }
        });
        return params;
    }

    private Object buildBaseBody(OperationModel operation, OperationOverride override) {
        return operation.requestBody().map(requestBody -> {
            Object value = valueGenerator.validValue(requestBody.schema());
            if (value instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = new LinkedHashMap<>((Map<String, Object>) map);
                override.fixedTestData().forEach((key, fixedValue) -> {
                    if (body.containsKey(key)) {
                        body.put(key, fixedValue);
                    }
                });
                return (Object) body;
            }
            return value;
        }).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Object withoutKey(Object body, String key) {
        if (!(body instanceof Map<?, ?> map)) {
            return body;
        }
        Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) map);
        copy.remove(key);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Object withReplacedKey(Object body, String key, Object value) {
        if (!(body instanceof Map<?, ?> map)) {
            return body;
        }
        Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) map);
        copy.put(key, value);
        return copy;
    }

    private boolean isFormatted(SchemaModel schema) {
        return schema.format() != null
                && (schema.format().equals("uuid") || schema.format().equals("email")
                || schema.format().equals("date-time") || schema.format().equals("date"));
    }

    private int expectedFor(OperationOverride override, TestCaseType type, int dynamicDefault) {
        Integer overridden = override.expectedStatusOverrides().get(type.name());
        return overridden != null ? overridden : dynamicDefault;
    }

    private Integer firstDocumented2xx(OperationModel operation) {
        TreeSet<Integer> sorted = new TreeSet<>();
        for (String status : operation.responses().keySet()) {
            if (status.length() == 3 && status.charAt(0) == '2') {
                try {
                    sorted.add(Integer.parseInt(status));
                } catch (NumberFormatException ignored) {
                    // niet-numerieke statuscode (bv. "2XX") wordt hier niet ondersteund
                }
            }
        }
        return sorted.isEmpty() ? null : sorted.first();
    }
}
