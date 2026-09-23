package dev.apitestkit.codegen;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.config.TestConfigLoader;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.model.SpecModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point voor het Maven-profiel -Pgenerate: leest de OpenAPI-spec en test-config.yaml,
 * en schrijft per tag een bewerkbare JUnit-testklasse naar src/test/java/generated.
 */
public final class CodegenMain {

    private CodegenMain() {
    }

    public static void main(String[] args) throws IOException {
        Path specPath = resolveSpecPath();
        Path configPath = specPath.resolveSibling("test-config.yaml");
        Path outputDir = resolveOutputDir();

        SpecModel spec = new OpenApiSpecLoader().load(specPath);
        TestConfig config = new TestConfigLoader().load(configPath, spec);
        List<TestCase> cases = new TestCaseGenerator().generate(spec, config);

        new GeneratedTestClassWriter().writeAll(cases, spec, specPath, outputDir);

        System.out.println("[codegen] " + cases.size() + " testcases gegenereerd naar " + outputDir.toAbsolutePath());
    }

    private static Path resolveSpecPath() {
        String value = System.getProperty("openapi.spec");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Geef het pad naar de OpenAPI-spec mee via -Dopenapi.spec=pad/naar/api.yaml");
        }
        return Path.of(value);
    }

    private static Path resolveOutputDir() {
        String value = System.getProperty("codegen.outputDir");
        if (value != null && !value.isBlank()) {
            return Path.of(value);
        }
        return Path.of("src/test/java/generated");
    }
}
