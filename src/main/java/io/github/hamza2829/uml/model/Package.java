package io.github.hamza2829.uml.model;

import java.util.List;

public record Package(String id, String name, String documentation, List<Package> packages,
                      List<ClassifierModel> classifiers, List<Association> associations) implements NamedElement {
    public Package {
        packages = List.copyOf(packages);
        classifiers = List.copyOf(classifiers);
        associations = List.copyOf(associations);
    }
}
