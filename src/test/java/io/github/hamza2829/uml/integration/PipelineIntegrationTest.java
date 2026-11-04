package io.github.hamza2829.uml.integration;

import io.github.hamza2829.uml.*;
import io.github.hamza2829.uml.cli.Main;
import io.github.hamza2829.uml.generation.*;
import io.github.hamza2829.uml.transformation.SysmlToUml;
import io.github.hamza2829.uml.validation.*;
import java.io.*;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PipelineIntegrationTest extends ModelFixture {
    @ParameterizedTest @ValueSource(strings = {"library", "sysml-rover", "sensor-tracker", "contracts"})
    void validSamplesPassWholeChainAndCompile(String name) throws Exception {
        var original = sample(name);
        assertFalse(RulesEngine.hasErrors(new RulesEngine().validate(original)));
        var flat = new SysmlToUml().transform(original);
        assertFalse(RulesEngine.hasErrors(new RulesEngine().validate(flat)));
        var generator = new JavaGenerator();
        var originalSources = generator.generate(original, temp.resolve("original/java"));
        var flatSources = generator.generate(flat, temp.resolve("flat/java"));
        assertEquals(original.classifiers().size(), originalSources.size());
        assertEquals(original.classifiers().size(), flatSources.size());
        try (var before = Compilation.compile(originalSources, temp.resolve("original/classes"));
             var after = Compilation.compile(flatSources, temp.resolve("flat/classes"))) {
            for (var qn : SysmlToUml.qualifiedNames(original).values()) assertNotNull(before.loadClass(qn));
            for (var qn : SysmlToUml.qualifiedNames(flat).values()) assertNotNull(after.loadClass(qn));
        }
        var out = new ByteArrayOutputStream();
        int exit = Main.run(new String[]{"generate", "models/" + name + ".xmi", "--output", temp.resolve("cli").toString()}, new PrintStream(out), System.err);
        assertEquals(0, exit, out.toString());
        assertTrue(Files.readString(temp.resolve("cli/model.puml")).endsWith("@enduml\n"));
    }

    @Test void invalidSampleFailsWithExactExpectedFindings() {
        var model = sample("invalid");
        var findings = new RulesEngine().validate(model);
        var counts = findings.stream().collect(java.util.stream.Collectors.groupingBy(Finding::rule, java.util.stream.Collectors.counting()));
        assertEquals(java.util.Map.of("unique-names", 1L, "missing-types", 1L, "multiplicities", 1L, "cyclic-generalization", 2L,
                "unnamed-elements", 2L, "naming-conventions", 3L, "blocks-without-parts", 1L), counts);
        assertThrows(GenerationException.class, () -> new JavaGenerator().generate(model, temp.resolve("invalid/java")));
        var out = new ByteArrayOutputStream();
        assertEquals(1, Main.run(new String[]{"generate", "models/invalid.xmi", "--output", temp.resolve("invalid").toString()}, new PrintStream(out), System.err));
        assertTrue(out.toString().contains("cyclic-generalization"));
        assertFalse(Files.exists(temp.resolve("invalid")));
    }
}
