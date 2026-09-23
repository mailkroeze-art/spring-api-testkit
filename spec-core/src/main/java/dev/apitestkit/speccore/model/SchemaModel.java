package dev.apitestkit.speccore.model;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SchemaModel(
        String type,
        String format,
        BigDecimal minimum,
        BigDecimal maximum,
        Integer minLength,
        Integer maxLength,
        List<String> required,
        Map<String, SchemaModel> properties,
        SchemaModel items,
        List<String> enumValues,
        Object example
) {
    public SchemaModel {
        required = required == null ? List.of() : List.copyOf(required);
        properties = OrderedMaps.copyOf(properties);
        enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
    }

    public static SchemaModel empty(String type) {
        return new SchemaModel(type, null, null, null, null, null, null, null, null, null, null);
    }
}
