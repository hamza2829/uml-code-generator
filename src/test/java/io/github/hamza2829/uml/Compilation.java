package io.github.hamza2829.uml;

import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import static org.junit.jupiter.api.Assertions.*;

public final class Compilation {
    private Compilation() { }

    public static URLClassLoader compile(List<Path> sources, Path output) throws Exception {
        Files.createDirectories(output);
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests require a Java 17 JDK with javax.tools.JavaCompiler");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            boolean success = compiler.getTask(null, manager, diagnostics, List.of("--release", "17", "-d", output.toString()),
                    null, manager.getJavaFileObjectsFromPaths(sources)).call();
            assertTrue(success, () -> "Generated code did not compile: " + diagnostics.getDiagnostics());
        }
        return new URLClassLoader(new URL[]{output.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }
}
