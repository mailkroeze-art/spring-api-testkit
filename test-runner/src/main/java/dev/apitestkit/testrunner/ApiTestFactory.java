package dev.apitestkit.testrunner;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.config.TestConfigLoader;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * JUnit 5 @TestFactory die de OpenAPI-spec inleest en op runtime een DynamicTest per
 * gegenereerde testcase produceert. Spec-pad via -Dopenapi.spec=pad/naar/api.yaml.
 * test-config.yaml wordt standaard naast de spec gezocht, tenzij -Dtestconfig.path is gezet.
 */
public class ApiTestFactory {

    @TestFactory
    Stream<DynamicTest> apiTests() {
        Path specPath = resolveSpecPath();
        SpecModel spec = new OpenApiSpecLoader().load(specPath);
        Path configPath = resolveConfigPath(specPath);
        TestConfig config = new TestConfigLoader().load(configPath, spec);
        List<TestCase> cases = new TestCaseGenerator().generate(spec, config);
        TestCaseExecutor executor = new TestCaseExecutor(spec, config, specPath.toUri().toString());

        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.displayName() + " -- " + testCase.description(),
                () -> executor.execute(testCase)));
    }

    private Path resolveSpecPath() {
        String value = System.getProperty("openapi.spec");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Geef het pad naar de OpenAPI-spec mee via -Dopenapi.spec=pad/naar/api.yaml");
        }
        return Path.of(value);
    }

    private Path resolveConfigPath(Path specPath) {
        String value = System.getProperty("testconfig.path");
        if (value != null && !value.isBlank()) {
            return Path.of(value);
        }
        return specPath.resolveSibling("test-config.yaml");
    }
}
