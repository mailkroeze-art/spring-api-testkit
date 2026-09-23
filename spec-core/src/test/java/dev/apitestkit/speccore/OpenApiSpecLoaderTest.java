package dev.apitestkit.speccore;

import dev.apitestkit.speccore.model.OperationModel;
import dev.apitestkit.speccore.model.ParameterModel;
import dev.apitestkit.speccore.model.SchemaModel;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiSpecLoaderTest {

    private static final Path FIXTURE = Path.of("src/test/resources/fixtures/sample-api.yaml");

    @Test
    void leestAlleOperatiesMetMethodePathEnOperationId() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        assertThat(spec.operations()).hasSize(3);
        OperationModel createPet = spec.operationById("createPet").orElseThrow();
        assertThat(createPet.httpMethod()).isEqualTo("POST");
        assertThat(createPet.path()).isEqualTo("/pets");
        assertThat(createPet.tags()).containsExactly("Pets");
    }

    @Test
    void operatieMetLegeSecurityOverrideVereistGeenAuth() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel listPets = spec.operationById("listPets").orElseThrow();
        assertThat(listPets.securitySchemeNames()).isEmpty();
    }

    @Test
    void operatieZonderOverrideErftGlobaleSecurity() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel createPet = spec.operationById("createPet").orElseThrow();
        assertThat(createPet.securitySchemeNames()).containsExactly("ApiKeyAuth");
        assertThat(spec.securityScheme("ApiKeyAuth")).isPresent();
    }

    @Test
    void parsetRequestBodyMetVerplichteVeldenEnConstraints() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel createPet = spec.operationById("createPet").orElseThrow();
        SchemaModel bodySchema = createPet.requestBody().orElseThrow().schema();

        assertThat(bodySchema.required()).containsExactlyInAnyOrder("name", "status");
        SchemaModel nameSchema = bodySchema.properties().get("name");
        assertThat(nameSchema.minLength()).isEqualTo(2);
        assertThat(nameSchema.maxLength()).isEqualTo(50);

        SchemaModel statusSchema = bodySchema.properties().get("status");
        assertThat(statusSchema.enumValues()).containsExactly("available", "pending", "sold");

        SchemaModel emailSchema = bodySchema.properties().get("email");
        assertThat(emailSchema.format()).isEqualTo("email");
    }

    @Test
    void parsetPathParameterMetUuidFormaat() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel getPetById = spec.operationById("getPetById").orElseThrow();
        List<ParameterModel> pathParams = getPetById.parameters().stream()
                .filter(p -> "path".equals(p.in()))
                .toList();

        assertThat(pathParams).hasSize(1);
        ParameterModel petId = pathParams.get(0);
        assertThat(petId.name()).isEqualTo("petId");
        assertThat(petId.required()).isTrue();
        assertThat(petId.schema().format()).isEqualTo("uuid");
    }

    @Test
    void parsetGedocumenteerdeResponseStatuscodes() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel createPet = spec.operationById("createPet").orElseThrow();
        assertThat(createPet.responses().keySet()).containsExactlyInAnyOrder("201", "400", "401");

        OperationModel getPetById = spec.operationById("getPetById").orElseThrow();
        assertThat(getPetById.responses().keySet()).containsExactlyInAnyOrder("200", "404");
    }

    @Test
    void queryParameterHeeftGrenswaarden() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel listPets = spec.operationById("listPets").orElseThrow();
        ParameterModel limit = listPets.parameters().get(0);

        assertThat(limit.required()).isFalse();
        assertThat(limit.schema().minimum()).isEqualByComparingTo("1");
        assertThat(limit.schema().maximum()).isEqualByComparingTo("100");
    }

    @Test
    void onbestaandSpecBestandGeeftDuidelijkeFoutmelding() {
        Path missing = Path.of("src/test/resources/fixtures/does-not-exist.yaml");

        assertThatThrownBy(() -> new OpenApiSpecLoader().load(missing))
                .isInstanceOf(SpecLoadException.class)
                .hasMessageContaining("does-not-exist.yaml");
    }

    @Test
    void requestBodyVoorbeeldWordtDoorgegeven() {
        SpecModel spec = new OpenApiSpecLoader().load(FIXTURE);

        OperationModel createPet = spec.operationById("createPet").orElseThrow();
        Optional<Object> example = createPet.requestBody().orElseThrow().example();

        assertThat(example).isPresent();
    }
}
