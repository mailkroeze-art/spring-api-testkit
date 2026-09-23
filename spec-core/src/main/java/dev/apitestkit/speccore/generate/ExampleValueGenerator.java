package dev.apitestkit.speccore.generate;

import dev.apitestkit.speccore.model.SchemaModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Genereert testwaarden uit een SchemaModel: geldige waarden (uit example/enum/schema),
 * grenswaarden (net binnen / net buiten min-max), en bewust ongeldige waarden
 * (verkeerd type, ongeldige enum, ongeldig formaat).
 */
public class ExampleValueGenerator {

    private static final int DEFAULT_STRING_LENGTH = 10;
    private static final String DEFAULT_STRING_VALUE = "teststring";

    public Object validValue(SchemaModel schema) {
        if (schema.example() != null) {
            return schema.example();
        }
        if (!schema.enumValues().isEmpty()) {
            return schema.enumValues().get(0);
        }

        String type = schema.type() == null ? "string" : schema.type();
        return switch (type) {
            case "integer" -> validNumber(schema, true);
            case "number" -> validNumber(schema, false);
            case "boolean" -> Boolean.TRUE;
            case "array" -> schema.items() == null ? java.util.List.of() : java.util.List.of(validValue(schema.items()));
            case "object" -> validObject(schema);
            default -> validString(schema);
        };
    }

    private Object validNumber(SchemaModel schema, boolean integer) {
        BigDecimal value;
        if (schema.minimum() != null) {
            value = schema.minimum();
        } else if (schema.maximum() != null) {
            value = schema.maximum();
        } else {
            value = BigDecimal.ONE;
        }
        if (integer) {
            return value.intValueExact();
        }
        return value.doubleValue();
    }

    private String validString(SchemaModel schema) {
        if ("uuid".equals(schema.format())) {
            return UUID.randomUUID().toString();
        }
        if ("email".equals(schema.format())) {
            return "gebruiker@voorbeeld.nl";
        }
        if ("date-time".equals(schema.format())) {
            return Instant.now().toString();
        }
        if ("date".equals(schema.format())) {
            return "2026-01-01";
        }

        int minLength = schema.minLength() == null ? 0 : schema.minLength();
        int maxLength = schema.maxLength() == null ? Integer.MAX_VALUE : schema.maxLength();
        int targetLength = Math.max(minLength, Math.min(DEFAULT_STRING_LENGTH, maxLength));
        return fixedLengthString(DEFAULT_STRING_VALUE, targetLength);
    }

    private Map<String, Object> validObject(SchemaModel schema) {
        Map<String, Object> value = new LinkedHashMap<>();
        schema.properties().forEach((name, propSchema) -> value.put(name, validValue(propSchema)));
        return value;
    }

    public Object invalidTypeValue(SchemaModel schema) {
        String type = schema.type() == null ? "string" : schema.type();
        return switch (type) {
            case "integer", "number" -> "niet-een-getal";
            case "boolean" -> "niet-een-boolean";
            case "array" -> "niet-een-array";
            case "object" -> "niet-een-object";
            default -> 1234567;
        };
    }

    public Object minBoundaryValue(SchemaModel schema) {
        if (isNumeric(schema) && schema.minimum() != null) {
            return toNumber(schema, schema.minimum());
        }
        if (schema.minLength() != null) {
            return fixedLengthString(DEFAULT_STRING_VALUE, schema.minLength());
        }
        return validValue(schema);
    }

    public Object belowMinBoundaryValue(SchemaModel schema) {
        if (isNumeric(schema) && schema.minimum() != null) {
            return toNumber(schema, schema.minimum().subtract(BigDecimal.ONE));
        }
        if (schema.minLength() != null && schema.minLength() > 0) {
            return fixedLengthString(DEFAULT_STRING_VALUE, schema.minLength() - 1);
        }
        return validValue(schema);
    }

    public Object maxBoundaryValue(SchemaModel schema) {
        if (isNumeric(schema) && schema.maximum() != null) {
            return toNumber(schema, schema.maximum());
        }
        if (schema.maxLength() != null) {
            return fixedLengthString(DEFAULT_STRING_VALUE, schema.maxLength());
        }
        return validValue(schema);
    }

    public Object aboveMaxBoundaryValue(SchemaModel schema) {
        if (isNumeric(schema) && schema.maximum() != null) {
            return toNumber(schema, schema.maximum().add(BigDecimal.ONE));
        }
        if (schema.maxLength() != null) {
            return fixedLengthString(DEFAULT_STRING_VALUE, schema.maxLength() + 1);
        }
        return validValue(schema);
    }

    public Object invalidEnumValue(SchemaModel schema) {
        String candidate = "__ONGELDIGE_ENUM_WAARDE__";
        while (schema.enumValues().contains(candidate)) {
            candidate = candidate + "_";
        }
        return candidate;
    }

    public Object invalidFormatValue(SchemaModel schema) {
        return switch (schema.format() == null ? "" : schema.format()) {
            case "uuid" -> "niet-een-geldige-uuid";
            case "email" -> "niet-een-emailadres";
            case "date-time" -> "niet-een-datumtijd";
            case "date" -> "niet-een-datum";
            default -> "!!ongeldig-formaat!!";
        };
    }

    private boolean isNumeric(SchemaModel schema) {
        return "integer".equals(schema.type()) || "number".equals(schema.type());
    }

    private Object toNumber(SchemaModel schema, BigDecimal value) {
        if ("integer".equals(schema.type())) {
            return value.intValueExact();
        }
        return value.doubleValue();
    }

    private String fixedLengthString(String base, int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(base);
        }
        return sb.substring(0, length);
    }
}
