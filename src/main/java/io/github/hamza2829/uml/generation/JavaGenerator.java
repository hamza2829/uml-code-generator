package io.github.hamza2829.uml.generation;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.transformation.SysmlToUml;
import io.github.hamza2829.uml.validation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JavaGenerator {
    private final TemplateEngine templates = new TemplateEngine();

    public List<Path> generate(Model model, Path directory) throws IOException {
        var findings = new RulesEngine().validate(model);
        if (RulesEngine.hasErrors(findings)) throw new GenerationException("Cannot generate invalid model: " + findings);
        var names = SysmlToUml.qualifiedNames(model);
        var files = new ArrayList<Path>();
        String classifierTemplate = resource("classifier.tpl");
        for (var c : model.classifiers()) {
            String qualified = names.get(c.id());
            String pkg = qualified.substring(0, qualified.lastIndexOf('.'));
            String text = templates.render(classifierTemplate, Map.of("PACKAGE", pkg, "DOC", javadoc(c.documentation(), ""),
                    "DECLARATION", declaration(model, c, names), "BODY", body(model, c, names)));
            Path file = directory.resolve(qualified.replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            files.add(file);
        }
        return List.copyOf(files);
    }

    private String declaration(Model model, ClassifierModel c, Map<String, String> names) {
        String keyword = switch (c.kind()) {
            case CLASS -> c.isAbstract() ? "abstract class" : "class";
            case INTERFACE -> "interface";
            case ENUMERATION -> "enum";
        };
        var parents = Stream.concat(c.generalizations().stream(), c.interfaces().stream()).distinct()
                .map(id -> model.classifier(id).orElseThrow()).toList();
        var classes = parents.stream().filter(p -> p.kind() == ClassifierModel.Kind.CLASS).map(p -> names.get(p.id())).toList();
        var interfaces = parents.stream().filter(p -> p.kind() == ClassifierModel.Kind.INTERFACE).map(p -> names.get(p.id())).toList();
        String result = "public " + keyword + " " + c.name();
        if (!classes.isEmpty()) result += " extends " + String.join(", ", classes);
        if (!interfaces.isEmpty()) result += (c.kind() == ClassifierModel.Kind.INTERFACE ? " extends " : " implements ") + String.join(", ", interfaces);
        return result;
    }

    private String body(Model model, ClassifierModel c, Map<String, String> names) throws IOException {
        var out = new StringBuilder();
        if (c.kind() == ClassifierModel.Kind.ENUMERATION) out.append("    ").append(String.join(", ", c.literals())).append(";\n\n");
        var properties = new LinkedHashMap<String, Property>();
        var operations = new LinkedHashMap<String, Operation>();
        collect(model, c, properties, operations, new HashSet<>(), false);
        String propertyTemplate = resource(c.kind() == ClassifierModel.Kind.INTERFACE ? "interface-property.tpl" : "property.tpl");
        var emitted = new HashSet<String>();
        for (var p : properties.values()) {
            String cap = Character.toUpperCase(p.name().charAt(0)) + p.name().substring(1);
            String type = javaType(p.type(), p.multiplicity(), names);
            out.append(templates.render(propertyTemplate, Map.of("DOC", javadoc(p.documentation(), "    "), "TYPE", type,
                    "NAME", p.name(), "CAP", cap, "INITIALIZER", p.multiplicity().isMany() ? " = new java.util.ArrayList<>()" : "")));
            emitted.add("get" + cap + "()");
            emitted.add("set" + cap + "(" + type + ")");
        }
        String operationTemplate = resource(c.kind() == ClassifierModel.Kind.INTERFACE ? "interface-operation.tpl" : "operation.tpl");
        for (var op : operations.values()) {
            String paramTypes = op.parameters().stream().map(p -> javaType(p.type(), p.multiplicity(), names)).collect(Collectors.joining(","));
            if (!emitted.add(op.name() + "(" + paramTypes + ")")) {
                throw new GenerationException("Operation collides with generated accessor: " + op.name() + " [XMI id=" + op.id() + "]");
            }
            String params = op.parameters().stream().map(p -> javaType(p.type(), p.multiplicity(), names) + " " + p.name()).collect(Collectors.joining(", "));
            out.append(templates.render(operationTemplate, Map.of("DOC", javadoc(op.documentation(), "    "), "TYPE",
                    javaType(op.returnType(), op.returnMultiplicity(), names), "NAME", op.name(), "PARAMETERS", params)));
        }
        return out.toString();
    }

    private void collect(Model model, ClassifierModel c, Map<String, Property> properties, Map<String, Operation> operations,
                         Set<String> seen, boolean inherited) {
        if (!seen.add(c.id())) return;
        var parents = Stream.concat(c.generalizations().stream(), c.interfaces().stream()).distinct().toList();
        for (var id : parents) {
            var parent = model.classifier(id).orElseThrow();
            // Implement interface contracts. Class parents already supply their members through Java inheritance.
            if (parent.kind() == ClassifierModel.Kind.INTERFACE) collect(model, parent, properties, operations, seen, true);
        }
        for (var p : c.properties()) mergeProperty(properties, p, inherited);
        for (var a : model.associations()) {
            if (a.source().equals(c.id()) && !a.targetRole().isEmpty()) {
                mergeProperty(properties, new Property(a.id(), a.targetRole(), a.documentation(), a.target(), a.targetMultiplicity(), Property.Role.ATTRIBUTE), true);
            }
            if (a.target().equals(c.id()) && !a.sourceRole().isEmpty()) {
                mergeProperty(properties, new Property(a.id(), a.sourceRole(), a.documentation(), a.source(), a.sourceMultiplicity(), Property.Role.ATTRIBUTE), true);
            }
        }
        for (var op : c.operations()) {
            String signature = op.name() + op.parameters().stream().map(p -> p.multiplicity().isMany() ? "List" : p.type()).toList();
            var old = operations.put(signature, op);
            if (old != null && (!old.returnType().equals(op.returnType()) || !old.returnMultiplicity().equals(op.returnMultiplicity()))) {
                throw new GenerationException("Conflicting inherited operation: " + op.name() + " [XMI id=" + op.id() + "]");
            }
        }
    }

    private void mergeProperty(Map<String, Property> properties, Property p, boolean inherited) {
        var old = properties.putIfAbsent(p.name(), p);
        if (old != null && (!old.type().equals(p.type()) || !old.multiplicity().equals(p.multiplicity()) || !inherited && old.id().equals(p.id()))) {
            throw new GenerationException("Conflicting property " + p.name() + " [XMI id=" + p.id() + "]");
        }
    }

    private String javaType(String type, Multiplicity multiplicity, Map<String, String> names) {
        String result = switch (type) {
            case "String" -> "java.lang.String";
            case "Integer", "UnlimitedNatural" -> "java.lang.Integer";
            case "Real" -> "java.lang.Double";
            case "Boolean" -> "java.lang.Boolean";
            case "void" -> "void";
            default -> names.get(type);
        };
        if (result == null) throw new GenerationException("Unknown type " + type);
        return multiplicity.isMany() ? "java.util.List<" + result + ">" : result;
    }

    private String resource(String name) throws IOException {
        try (var in = JavaGenerator.class.getResourceAsStream("/templates/" + name)) {
            if (in == null) throw new IOException("Missing generator template " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String javadoc(String doc, String indent) {
        if (doc.isBlank()) return "";
        String safe = doc.replace("&", "&amp;").replace("\\", "&#92;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("*/", "*&#47;").replace("@", "&#64;");
        return indent + "/**\n" + safe.lines().map(s -> indent + " * " + s + "\n").collect(Collectors.joining()) + indent + " */\n";
    }
}
