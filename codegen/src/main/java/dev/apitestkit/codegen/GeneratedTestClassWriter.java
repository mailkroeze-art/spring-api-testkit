package dev.apitestkit.codegen;

import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.model.SpecModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schrijft per OpenAPI-tag een bewerkbare JUnit 5-testklasse naar de opgegeven outputmap.
 * Het gegenereerde blok (tussen de markers) wordt bij elke run overschreven; alles wat een
 * gebruiker daaronder toevoegt blijft bij een volgende generatie behouden.
 */
public class GeneratedTestClassWriter {

    private static final String CUSTOM_ZONE_DEFAULT =
            "    // Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.\n\n";
    private static final String END_MARKER = "// <<< EINDE GEGENEREERD BLOK";

    private final JavaLiteralRenderer renderer = new JavaLiteralRenderer();

    public void writeAll(List<TestCase> cases, SpecModel spec, Path specPath, Path outputDir) throws IOException {
        Map<String, List<TestCase>> byTag = new LinkedHashMap<>();
        for (TestCase testCase : cases) {
            byTag.computeIfAbsent(firstTag(spec, testCase.operationId()), t -> new ArrayList<>()).add(testCase);
        }

        Files.createDirectories(outputDir);
        for (Map.Entry<String, List<TestCase>> entry : byTag.entrySet()) {
            String className = sanitizeClassName(entry.getKey()) + "GeneratedTest";
            Path file = outputDir.resolve(className + ".java");
            String generatedPart = buildGeneratedPart(className, entry.getValue(), specPath);
            String customZone = extractCustomZone(file);
            Files.writeString(file, generatedPart + customZone + "}\n");
        }
    }

    private String firstTag(SpecModel spec, String operationId) {
        return spec.operationById(operationId)
                .map(op -> op.tags().isEmpty() ? "Default" : op.tags().get(0))
                .orElse("Default");
    }

    private String sanitizeClassName(String tag) {
        String cleaned = tag.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            return "Default";
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private String sanitizeMethodName(String operationId) {
        return operationId.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String buildGeneratedPart(String className, List<TestCase> cases, Path specPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("package generated;\n\n");
        sb.append("import dev.apitestkit.speccore.OpenApiSpecLoader;\n");
        sb.append("import dev.apitestkit.speccore.config.TestConfig;\n");
        sb.append("import dev.apitestkit.speccore.config.TestConfigLoader;\n");
        sb.append("import dev.apitestkit.speccore.generate.AuthMode;\n");
        sb.append("import dev.apitestkit.speccore.generate.TestCase;\n");
        sb.append("import dev.apitestkit.speccore.generate.TestCaseType;\n");
        sb.append("import dev.apitestkit.speccore.model.SpecModel;\n");
        sb.append("import dev.apitestkit.testrunner.TestCaseExecutor;\n");
        sb.append("import org.junit.jupiter.api.BeforeAll;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("import java.nio.file.Path;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n");
        sb.append(" * GEGENEREERD door codegen (-Pgenerate) op basis van ").append(specPath.getFileName()).append(".\n");
        sb.append(" * Veilig om aan te passen: voeg eigen tests toe ONDER de \"EINDE GEGENEREERD BLOK\"-marker.\n");
        sb.append(" * Alles daaronder blijft bij een volgende generatie behouden; het blok zelf wordt overschreven.\n");
        sb.append(" */\n");
        sb.append("class ").append(className).append(" {\n\n");
        sb.append("    private static TestCaseExecutor executor;\n\n");
        sb.append("    @BeforeAll\n");
        sb.append("    static void setUpFramework() {\n");
        sb.append("        Path specPath = Path.of(").append(renderer.render(specPath.toString())).append(");\n");
        sb.append("        SpecModel spec = new OpenApiSpecLoader().load(specPath);\n");
        sb.append("        TestConfig config = new TestConfigLoader().load(specPath.resolveSibling(\"test-config.yaml\"), spec);\n");
        sb.append("        executor = new TestCaseExecutor(spec, config, specPath.toUri().toString());\n");
        sb.append("    }\n\n");
        sb.append("    // >>> GEGENEREERD -- niet handmatig bewerken, wordt overschreven bij opnieuw genereren\n");

        Map<String, Integer> counters = new LinkedHashMap<>();
        for (TestCase testCase : cases) {
            String key = testCase.operationId() + "_" + testCase.type().name();
            int index = counters.merge(key, 1, Integer::sum);
            String methodName = sanitizeMethodName(testCase.operationId()) + "_" + testCase.type().name().toLowerCase()
                    + (index > 1 ? "_" + index : "");

            sb.append("\n    // ").append(testCase.description()).append("\n");
            sb.append("    @Test\n");
            sb.append("    void ").append(methodName).append("() {\n");
            sb.append("        executor.execute(new TestCase(\n");
            sb.append("                ").append(renderer.render(testCase.operationId())).append(",\n");
            sb.append("                ").append(renderer.render(testCase.httpMethod())).append(",\n");
            sb.append("                ").append(renderer.render(testCase.path())).append(",\n");
            sb.append("                TestCaseType.").append(testCase.type().name()).append(",\n");
            sb.append("                ").append(renderer.render(testCase.description())).append(",\n");
            sb.append("                ").append(renderer.render(testCase.pathParams())).append(",\n");
            sb.append("                ").append(renderer.render(testCase.queryParams())).append(",\n");
            sb.append("                ").append(renderer.render(testCase.requestBody())).append(",\n");
            sb.append("                AuthMode.").append(testCase.authMode().name()).append(",\n");
            sb.append("                ").append(testCase.expectedStatusCode()).append(",\n");
            sb.append("                ").append(renderer.render(testCase.setupOperationId())).append("));\n");
            sb.append("    }\n");
        }

        sb.append("\n    ").append(END_MARKER).append("\n\n");
        return sb.toString();
    }

    private String extractCustomZone(Path file) throws IOException {
        if (!Files.exists(file)) {
            return CUSTOM_ZONE_DEFAULT;
        }
        String content = Files.readString(file);
        int markerIndex = content.indexOf(END_MARKER);
        if (markerIndex < 0) {
            return CUSTOM_ZONE_DEFAULT;
        }
        int afterMarkerLine = content.indexOf('\n', markerIndex);
        int lastBrace = content.lastIndexOf('}');
        if (afterMarkerLine < 0 || lastBrace < afterMarkerLine) {
            return CUSTOM_ZONE_DEFAULT;
        }
        return content.substring(afterMarkerLine + 1, lastBrace);
    }
}
