package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;
import java.util.stream.Stream;

/** Reject structural cases that Java single inheritance cannot represent. */
public final class JavaStructureRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var result = new ArrayList<Finding>();
        for (var c : model.classifiers()) {
            var parents = Stream.concat(c.generalizations().stream(), c.interfaces().stream())
                    .map(model::classifier).flatMap(Optional::stream).toList();
            long classes = parents.stream().filter(p -> p.kind() == ClassifierModel.Kind.CLASS).count();
            if (classes > 1 || (c.kind() == ClassifierModel.Kind.INTERFACE && classes > 0)
                    || (c.kind() == ClassifierModel.Kind.ENUMERATION && !parents.isEmpty())
                    || parents.stream().anyMatch(p -> p.kind() == ClassifierModel.Kind.ENUMERATION)) {
                result.add(Finding.error("java-structure", c, "Inheritance cannot be represented in Java"));
            }
            if (c.interfaces().stream().map(model::classifier).flatMap(Optional::stream)
                    .anyMatch(i -> i.kind() != ClassifierModel.Kind.INTERFACE)) {
                result.add(Finding.error("java-structure", c, "Interface realization must target an interface"));
            }
            var signatures = new HashSet<String>();
            for (var op : c.operations()) {
                String signature = op.name() + op.parameters().stream().map(p -> p.multiplicity().isMany() ? "List" : p.type()).toList();
                if (!signatures.add(signature)) result.add(Finding.error("java-structure", op, "Duplicate Java operation signature after type erasure"));
                if (Set.of("getClass", "wait", "notify", "notifyAll").contains(op.name())) {
                    result.add(Finding.error("java-structure", op, "Operation conflicts with a final Object method"));
                }
            }
            if (c.properties().stream().anyMatch(p -> p.name().equals("class"))) {
                result.add(Finding.error("java-structure", c, "Property would override final Object.getClass()"));
            }
            for (var p : c.properties()) if (p.role() == Property.Role.PART
                    && model.classifier(p.type()).map(t -> t.kind() != ClassifierModel.Kind.CLASS).orElse(true)) {
                result.add(Finding.error("java-structure", p, "Part property must refer to a class or block"));
            }
        }
        return result;
    }
}
