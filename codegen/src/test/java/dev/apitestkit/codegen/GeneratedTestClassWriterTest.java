package dev.apitestkit.codegen;

import dev.apitestkit.speccore.OpenApiSpecLoader;
import dev.apitestkit.speccore.config.TestConfig;
import dev.apitestkit.speccore.generate.TestCase;
import dev.apitestkit.speccore.generate.TestCaseGenerator;
import dev.apitestkit.speccore.model.SpecModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedTestClassWriterTest {

    private static final Path SPEC_PATH = Path.of("src/test/resources/fixtures/sample-api.yaml").toAbsolutePath();

    @Test
    void schrijftEenTestklasseBestandPerTagMetGeneratedBlokMarkers(@TempDir Path outputDir) throws IOException {
        SpecModel spec = new OpenApiSpecLoader().load(SPEC_PATH);
        List<TestCase> cases = new TestCaseGenerator().generate(spec, TestConfig.defaults());

        new GeneratedTestClassWriter().writeAll(cases, spec, SPEC_PATH, outputDir);

        Path generatedFile = outputDir.resolve("PetsGeneratedTest.java");
        assertThat(generatedFile).exists();
        String content = Files.readString(generatedFile);
        assertThat(content).contains("package generated;");
        assertThat(content).contains("class PetsGeneratedTest");
        assertThat(content).contains("// >>> GEGENEREERD");
        assertThat(content).contains("// <<< EINDE GEGENEREERD BLOK");
        assertThat(content).contains("@Test");
    }

    @Test
    void behoudtHandmatigToegevoegdeCodeNaEenTweedeGeneratie(@TempDir Path outputDir) throws IOException {
        SpecModel spec = new OpenApiSpecLoader().load(SPEC_PATH);
        List<TestCase> cases = new TestCaseGenerator().generate(spec, TestConfig.defaults());
        GeneratedTestClassWriter writer = new GeneratedTestClassWriter();

        writer.writeAll(cases, spec, SPEC_PATH, outputDir);
        Path generatedFile = outputDir.resolve("PetsGeneratedTest.java");
        String firstContent = Files.readString(generatedFile);
        String withCustomTest = firstContent.replace(
                "// Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.",
                "// Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.\n"
                        + "    @Test\n    void mijnEigenBusinessRegelTest() { /* aangepast door gebruiker */ }");
        Files.writeString(generatedFile, withCustomTest);

        writer.writeAll(cases, spec, SPEC_PATH, outputDir);
        String regenerated = Files.readString(generatedFile);

        assertThat(regenerated).contains("mijnEigenBusinessRegelTest");
    }
}
