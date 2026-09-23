package dev.apitestkit.speccore.config;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestConfigLoaderTest {

    private static final Path SPEC = Path.of("src/test/resources/fixtures/sample-api.yaml");
    private static final Path CONFIG_DIR = Path.of("src/test/resources/fixtures/config");

    private SpecModel spec() {
        return new OpenApiSpecLoader().load(SPEC);
    }

    @Test
    void gebruiktDefaultsAlsConfigBestandOntbreekt() {
        TestConfig config = new TestConfigLoader().load(Path.of("src/test/resources/fixtures/config/does-not-exist.yaml"), spec());

        assertThat(config.baseUrl()).isNull();
        assertThat(config.operationOverrides()).isEmpty();
        assertThat(config.isCaseTypeEnabled("UNAUTHORIZED")).isTrue();
    }

    @Test
    void leestBaseUrlAuthEnOperationOverrides() {
        TestConfig config = new TestConfigLoader().load(CONFIG_DIR.resolve("valid-config.yaml"), spec());

        assertThat(config.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.auth()).isNotNull();
        assertThat(config.auth().type()).isEqualTo("bearer");
        assertThat(config.auth().settings()).containsEntry("tokenEnv", "API_TOKEN");

        OperationOverride createPetOverride = config.operationOverrides().get("createPet");
        assertThat(createPetOverride.skip()).isFalse();
        assertThat(createPetOverride.expectedStatusOverrides()).containsEntry("MISSING_REQUIRED", 422);
        assertThat(createPetOverride.fixedTestData()).containsEntry("name", "Fido");

        OperationOverride getPetOverride = config.operationOverrides().get("getPetById");
        assertThat(getPetOverride.setupDependsOn()).isEqualTo("createPet");
    }

    @Test
    void caseTypeUitzettenWordtGelezen() {
        TestConfig config = new TestConfigLoader().load(CONFIG_DIR.resolve("valid-config.yaml"), spec());

        assertThat(config.isCaseTypeEnabled("UNAUTHORIZED")).isFalse();
        assertThat(config.isCaseTypeEnabled("HAPPY_PATH")).isTrue();
    }

    @Test
    void onbekendeOperationIdGeeftFoutmeldingMetBestandEnRegel() {
        assertThatThrownBy(() -> new TestConfigLoader().load(CONFIG_DIR.resolve("unknown-operation-config.yaml"), spec()))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("doesNotExist")
                .hasMessageContaining("unknown-operation-config.yaml")
                .hasMessageContaining("regel 2");
    }

    @Test
    void onbekendeTopLevelSleutelGeeftFoutmeldingMetBestandEnRegel() {
        assertThatThrownBy(() -> new TestConfigLoader().load(CONFIG_DIR.resolve("unknown-toplevel-key-config.yaml"), spec()))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("totallyUnknown")
                .hasMessageContaining("unknown-toplevel-key-config.yaml")
                .hasMessageContaining("regel 2");
    }

    @Test
    void onbekendeOperationSleutelGeeftFoutmeldingMetBestandEnRegel() {
        assertThatThrownBy(() -> new TestConfigLoader().load(CONFIG_DIR.resolve("unknown-operation-key-config.yaml"), spec()))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("totallyUnknown")
                .hasMessageContaining("unknown-operation-key-config.yaml")
                .hasMessageContaining("regel 3");
    }
}
