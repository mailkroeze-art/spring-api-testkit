package dev.apitestkit.codegen;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JavaLiteralRendererTest {

    private final JavaLiteralRenderer renderer = new JavaLiteralRenderer();

    @Test
    void rendertNullAlsNullLiteral() {
        assertThat(renderer.render(null)).isEqualTo("null");
    }

    @Test
    void rendertStringMetEscapedQuotesEnBackslashes() {
        assertThat(renderer.render("hoi \"wereld\" \\ test")).isEqualTo("\"hoi \\\"wereld\\\" \\\\ test\"");
    }

    @Test
    void rendertIntegerZonderSuffix() {
        assertThat(renderer.render(42)).isEqualTo("42");
    }

    @Test
    void rendertDoubleMetDSuffix() {
        assertThat(renderer.render(3.5)).isEqualTo("3.5d");
    }

    @Test
    void rendertBooleanLetterlijk() {
        assertThat(renderer.render(true)).isEqualTo("true");
    }

    @Test
    void rendertLegeMapAlsMapOf() {
        assertThat(renderer.render(Map.of())).isEqualTo("Map.of()");
    }

    @Test
    void rendertMapMetEenSleutelWaardePaar() {
        assertThat(renderer.render(Map.of("name", "Rex"))).isEqualTo("Map.of(\"name\", \"Rex\")");
    }

    @Test
    void rendertLegeLijstAlsListOf() {
        assertThat(renderer.render(List.of())).isEqualTo("List.of()");
    }

    @Test
    void rendertLijstMetWaarden() {
        assertThat(renderer.render(List.of(1, 2))).isEqualTo("List.of(1, 2)");
    }
}
