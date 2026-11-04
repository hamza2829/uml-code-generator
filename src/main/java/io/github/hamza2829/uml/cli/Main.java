package io.github.hamza2829.uml.cli;

import io.github.hamza2829.uml.parser.*;
import io.github.hamza2829.uml.generation.*;
import io.github.hamza2829.uml.transformation.SysmlToUml;
import io.github.hamza2829.uml.validation.*;
import java.io.*;
import java.nio.file.*;

public final class Main {
    private Main() { }

    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && args[0].equals("--help")) { usage(out); return 0; }
        if (args.length < 2 || (!args[0].equals("validate") && !args[0].equals("generate"))) { usage(err); return 2; }
        try {
            Path output = null;
            boolean flatten = false;
            for (int i = 2; i < args.length; i++) {
                if (args[0].equals("generate") && args[i].equals("--output") && i + 1 < args.length && output == null) output = Path.of(args[++i]);
                else if (args[0].equals("generate") && args[i].equals("--flatten") && !flatten) flatten = true;
                else { usage(err); return 2; }
            }
            if (args[0].equals("generate") && output == null) { usage(err); return 2; }
            var model = new XmiParser().parse(Path.of(args[1]));
            var findings = new RulesEngine().validate(model);
            findings.forEach(out::println);
            if (RulesEngine.hasErrors(findings)) return 1;
            if (args[0].equals("generate")) {
                if (flatten) model = new SysmlToUml().transform(model);
                var files = new JavaGenerator().generate(model, output.resolve("java"));
                new PlantUmlGenerator().generate(model, output.resolve("model.puml"));
                out.println("Generated " + files.size() + " Java files and " + output.resolve("model.puml"));
            } else out.println("Validation passed: " + model.classifiers().size() + " classifiers");
            return 0;
        } catch (ModelParseException | GenerationException | IOException | InvalidPathException e) {
            err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }

    private static void usage(PrintStream out) {
        out.println("Usage: java -jar uml-code-generator-1.0.0.jar validate MODEL.xmi");
        out.println("       java -jar uml-code-generator-1.0.0.jar generate MODEL.xmi --output DIRECTORY [--flatten]");
    }
}
