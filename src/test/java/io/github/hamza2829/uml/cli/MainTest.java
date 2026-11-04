package io.github.hamza2829.uml.cli;

import io.github.hamza2829.uml.ModelFixture;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class MainTest extends ModelFixture {
    private record Result(int exit, String out, String err) { }
    private Result run(String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int exit = Main.run(args, new PrintStream(out), new PrintStream(err));
        return new Result(exit, out.toString(java.nio.charset.StandardCharsets.UTF_8), err.toString(java.nio.charset.StandardCharsets.UTF_8));
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidArgs() {
        return java.util.stream.Stream.of(new String[]{}, new String[]{"validate"}, new String[]{"bogus", "model"},
                new String[]{"generate", "models/library.xmi"}, new String[]{"generate", "models/library.xmi", "--output"},
                new String[]{"validate", "models/library.xmi", "--flatten"}, new String[]{"generate", "models/library.xmi", "--bogus"},
                new String[]{"generate", "models/library.xmi", "--flatten", "--flatten"},
                new String[]{"generate", "models/library.xmi", "--output", "a", "--output", "b"})
                .map(args -> org.junit.jupiter.params.provider.Arguments.of((Object) args));
    }
    @ParameterizedTest @MethodSource("invalidArgs") void badUsageReturnsTwo(String[] args) {
        var r = run(args);
        assertEquals(2, r.exit());
        assertTrue(r.err().contains("Usage:"));
    }
    @Test void helpReturnsZero() { assertEquals(0, run("--help").exit()); }
    @Test void validationReportsErrorsAndNoStackTrace() {
        var r = run("validate", "models/invalid.xmi");
        assertEquals(1, r.exit());
        assertTrue(r.out().contains("ERROR [missing-types] Upper [XMI id=missingType]"));
        assertEquals("", r.err());
        assertFalse(r.out().contains("Exception"));
    }
    @Test void warningsDoNotFailValidation() {
        var r = run("validate", "models/sysml-rover.xmi");
        assertEquals(0, r.exit());
        assertTrue(r.out().contains("WARNING [blocks-without-parts]"));
        assertTrue(r.out().contains("Validation passed: 3 classifiers"));
    }
    @Test void generatesJavaAndPlantumlWithFlattenOption() {
        var r = run("generate", "models/sysml-rover.xmi", "--output", temp.toString(), "--flatten");
        assertEquals(0, r.exit(), r.err());
        assertTrue(Files.exists(temp.resolve("java/flat/Rover.java")));
        assertTrue(Files.exists(temp.resolve("model.puml")));
        assertTrue(r.out().contains("Generated 3 Java files"));
    }
    @Test void invalidInputGeneratesNothing() {
        assertEquals(1, run("generate", "models/invalid.xmi", "--output", temp.resolve("out").toString()).exit());
        assertFalse(Files.exists(temp.resolve("out")));
    }
    @Test void malformedXmlIsContextualWithoutStackTrace() throws Exception {
        Path input = Files.writeString(temp.resolve("broken.xmi"), "<xmi");
        var r = run("validate", input.toString());
        assertEquals(1, r.exit());
        assertTrue(r.err().contains("broken.xmi [XMI id=<document>]"));
        assertFalse(r.err().contains("\tat "));
    }
    @Test void missingFileIsReported() {
        var r = run("validate", temp.resolve("absent.xmi").toString());
        assertEquals(1, r.exit());
        assertTrue(r.err().contains("absent.xmi"));
    }
    @Test void outputFailureIsReportedWithoutStackTrace() throws Exception {
        Path output = Files.writeString(temp.resolve("occupied"), "file");
        var r = run("generate", "models/library.xmi", "--output", output.toString());
        assertEquals(1, r.exit());
        assertTrue(r.err().startsWith("ERROR:"));
        assertFalse(r.err().contains("\tat "));
    }
    @Test void invalidPathIsReportedWithoutStackTrace() {
        var r = run("generate", "models/library.xmi", "--output", "bad\u0000path");
        assertEquals(1, r.exit());
        assertTrue(r.err().startsWith("ERROR:"));
    }
}
