package io.github.hamza2829.uml.generation;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.transformation.SysmlToUml;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class PlantUmlGenerator {
    public String render(Model model) {
        var names = SysmlToUml.qualifiedNames(model);
        var aliases = new LinkedHashMap<String, String>();
        model.classifiers().forEach(c -> aliases.put(c.id(), "C" + aliases.size()));
        var out = new StringBuilder("@startuml\nhide empty members\n");
        for (var c : model.classifiers()) {
            String keyword = switch (c.kind()) {
                case CLASS -> c.isAbstract() ? "abstract class" : "class";
                case INTERFACE -> "interface";
                case ENUMERATION -> "enum";
            };
            out.append(keyword).append(" \"").append(escape(names.get(c.id()))).append("\" as ").append(aliases.get(c.id()));
            if (c.isBlock()) out.append(" <<block>>");
            out.append(" {\n");
            c.literals().forEach(l -> out.append("  ").append(escape(l)).append("\n"));
            c.properties().forEach(p -> out.append("  ").append(escape(p.name())).append(" : ")
                    .append(escape(names.getOrDefault(p.type(), p.type()))).append(" [").append(p.multiplicity()).append("]\n"));
            c.operations().forEach(op -> out.append("  ").append(escape(op.name())).append("(")
                    .append(op.parameters().stream().map(p -> escape(p.name()) + " : " + escape(names.getOrDefault(p.type(), p.type())))
                            .collect(Collectors.joining(", "))).append(") : ").append(escape(names.getOrDefault(op.returnType(), op.returnType()))).append("\n"));
            out.append("}\n");
            c.generalizations().forEach(p -> out.append(aliases.get(c.id())).append(" --|> ").append(aliases.get(p)).append("\n"));
            c.interfaces().forEach(p -> out.append(aliases.get(c.id())).append(" ..|> ").append(aliases.get(p)).append("\n"));
            for (var p : c.properties()) if (p.role() == Property.Role.PART) {
                out.append(aliases.get(c.id())).append(" *-- \"").append(p.multiplicity()).append("\" ")
                        .append(aliases.get(p.type())).append(" : ").append(escape(p.name())).append("\n");
            }
        }
        for (var a : model.associations()) out.append(aliases.get(a.source())).append(" \"").append(a.sourceMultiplicity())
                .append(" ").append(escape(a.sourceRole())).append("\" -- \"").append(a.targetMultiplicity()).append(" ")
                .append(escape(a.targetRole())).append("\" ").append(aliases.get(a.target())).append(" : ").append(escape(a.name())).append("\n");
        return out.append("@enduml\n").toString();
    }

    public void generate(Model model, Path file) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, render(model));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
