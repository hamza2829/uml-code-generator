package io.github.hamza2829.uml.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Model(String source, List<Package> packages) {
    public static final List<String> PRIMITIVES = List.of("String", "Integer", "Real", "Boolean", "UnlimitedNatural");

    public Model { packages = List.copyOf(packages); }

    public List<Package> allPackages() {
        var result = new ArrayList<Package>();
        packages.forEach(p -> visit(p, result));
        return List.copyOf(result);
    }

    private static void visit(Package p, List<Package> result) {
        result.add(p);
        p.packages().forEach(child -> visit(child, result));
    }

    public List<ClassifierModel> classifiers() {
        return allPackages().stream().flatMap(p -> p.classifiers().stream()).toList();
    }

    public List<Association> associations() {
        return allPackages().stream().flatMap(p -> p.associations().stream()).toList();
    }

    public Optional<ClassifierModel> classifier(String id) {
        return classifiers().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public List<NamedElement> elements() {
        var result = new ArrayList<NamedElement>(allPackages());
        result.addAll(associations());
        for (var c : classifiers()) {
            result.add(c);
            result.addAll(c.properties());
            for (var op : c.operations()) {
                result.add(op);
                result.addAll(op.parameters());
            }
        }
        return List.copyOf(result);
    }
}
