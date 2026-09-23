package dev.apitestkit.speccore;

import dev.apitestkit.speccore.model.OperationModel;
import dev.apitestkit.speccore.model.SchemaModel;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AllOfSchemaMergeTest {

    @Test
    void voegtEigenschappenEnVerplichteVeldenUitAlleAllOfTakkenSamen() {
        SpecModel spec = new OpenApiSpecLoader().load(Path.of("src/test/resources/fixtures/allof-sample.yaml"));

        OperationModel getWidget = spec.operationById("getWidget").orElseThrow();
        SchemaModel widgetSchema = getWidget.responses().get("200").schema();

        assertThat(widgetSchema.properties()).containsKeys("label", "id");
        assertThat(widgetSchema.required()).containsExactlyInAnyOrder("label", "id");
        assertThat(widgetSchema.properties().get("id").format()).isEqualTo("uuid");
    }
}
