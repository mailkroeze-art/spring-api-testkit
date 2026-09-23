package dev.apitestkit.speccore.generate;

import dev.apitestkit.speccore.model.SchemaModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleValueGeneratorTest {

    private final ExampleValueGenerator generator = new ExampleValueGenerator();

    @Test
    void gebruiktExampleUitSchemaAlsDieAanwezigIs() {
        SchemaModel schema = new SchemaModel("string", null, null, null, null, null,
                null, null, null, null, "voorbeeldwaarde");

        assertThat(generator.validValue(schema)).isEqualTo("voorbeeldwaarde");
    }

    @Test
    void genereertGeldigeUuidStringVoorUuidFormaat() {
        SchemaModel schema = new SchemaModel("string", "uuid", null, null, null, null,
                null, null, null, null, null);

        Object value = generator.validValue(schema);
        assertThat(value).asString().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void genereertGeldigEmailadresVoorEmailFormaat() {
        SchemaModel schema = new SchemaModel("string", "email", null, null, null, null,
                null, null, null, null, null);

        assertThat(generator.validValue(schema)).asString().contains("@");
    }

    @Test
    void genereertGeldigeDateTimeVoorDateTimeFormaat() {
        SchemaModel schema = new SchemaModel("string", "date-time", null, null, null, null,
                null, null, null, null, null);

        assertThat(generator.validValue(schema)).asString().matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }

    @Test
    void respecteertMinLengthEnMaxLengthBijStandaardStringgeneratie() {
        SchemaModel schema = new SchemaModel("string", null, null, null, 5, 8,
                null, null, null, null, null);

        String value = (String) generator.validValue(schema);
        assertThat(value.length()).isBetween(5, 8);
    }

    @Test
    void gebruiktMinimumAlsGeldigeIntegerWaarde() {
        SchemaModel schema = new SchemaModel("integer", null, new BigDecimal("10"), new BigDecimal("20"),
                null, null, null, null, null, null, null);

        assertThat(generator.validValue(schema)).isEqualTo(10);
    }

    @Test
    void gebruiktEersteEnumWaardeAlsGeenExampleAanwezigIs() {
        SchemaModel schema = new SchemaModel("string", null, null, null, null, null,
                null, null, null, List.of("available", "pending", "sold"), null);

        assertThat(generator.validValue(schema)).isEqualTo("available");
    }

    @Test
    void genereertVerplichteObjectVeldenRecursief() {
        SchemaModel nameSchema = new SchemaModel("string", null, null, null, null, null,
                null, null, null, null, null);
        SchemaModel objectSchema = new SchemaModel("object", null, null, null, null, null,
                List.of("name"), Map.of("name", nameSchema), null, null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> value = (Map<String, Object>) generator.validValue(objectSchema);
        assertThat(value).containsKey("name");
    }

    @Test
    void invalidTypeValueGeeftStringVoorIntegerSchema() {
        SchemaModel schema = new SchemaModel("integer", null, null, null, null, null,
                null, null, null, null, null);

        assertThat(generator.invalidTypeValue(schema)).isInstanceOf(String.class);
    }

    @Test
    void invalidTypeValueGeeftGetalVoorStringSchema() {
        SchemaModel schema = new SchemaModel("string", null, null, null, null, null,
                null, null, null, null, null);

        assertThat(generator.invalidTypeValue(schema)).isInstanceOf(Number.class);
    }

    @Test
    void minBoundaryValueGeeftExacteMinimumWaarde() {
        SchemaModel schema = new SchemaModel("integer", null, new BigDecimal("5"), new BigDecimal("50"),
                null, null, null, null, null, null, null);

        assertThat(generator.minBoundaryValue(schema)).isEqualTo(5);
    }

    @Test
    void belowMinBoundaryValueGaatOnderDeMinimumwaarde() {
        SchemaModel schema = new SchemaModel("integer", null, new BigDecimal("5"), new BigDecimal("50"),
                null, null, null, null, null, null, null);

        assertThat(generator.belowMinBoundaryValue(schema)).isEqualTo(4);
    }

    @Test
    void maxBoundaryValueGeeftExacteMaximumwaarde() {
        SchemaModel schema = new SchemaModel("integer", null, new BigDecimal("5"), new BigDecimal("50"),
                null, null, null, null, null, null, null);

        assertThat(generator.maxBoundaryValue(schema)).isEqualTo(50);
    }

    @Test
    void aboveMaxBoundaryValueGaatBovenDeMaximumwaarde() {
        SchemaModel schema = new SchemaModel("integer", null, new BigDecimal("5"), new BigDecimal("50"),
                null, null, null, null, null, null, null);

        assertThat(generator.aboveMaxBoundaryValue(schema)).isEqualTo(51);
    }

    @Test
    void stringBoundaryWaardenRespecterenMinLengthEnMaxLength() {
        SchemaModel schema = new SchemaModel("string", null, null, null, 3, 6,
                null, null, null, null, null);

        assertThat(((String) generator.minBoundaryValue(schema)).length()).isEqualTo(3);
        assertThat(((String) generator.belowMinBoundaryValue(schema)).length()).isEqualTo(2);
        assertThat(((String) generator.maxBoundaryValue(schema)).length()).isEqualTo(6);
        assertThat(((String) generator.aboveMaxBoundaryValue(schema)).length()).isEqualTo(7);
    }

    @Test
    void invalidEnumValueZitNietInDeToegestaneWaarden() {
        SchemaModel schema = new SchemaModel("string", null, null, null, null, null,
                null, null, null, List.of("available", "pending", "sold"), null);

        assertThat(generator.invalidEnumValue(schema)).asString().isNotIn("available", "pending", "sold");
    }

    @Test
    void invalidFormatValueVoorUuidIsGeenGeldigeUuid() {
        SchemaModel schema = new SchemaModel("string", "uuid", null, null, null, null,
                null, null, null, null, null);

        assertThat(generator.invalidFormatValue(schema)).asString()
                .doesNotMatch("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
