package io.github.hamza2829.uml.model;

import java.util.List;

public record ClassifierModel(String id, String name, String documentation, Kind kind,
                              List<Property> properties, List<Operation> operations, List<String> literals,
                              List<String> generalizations, List<String> interfaces,
                              List<Stereotype> stereotypes, boolean isAbstract) implements NamedElement {
    public enum Kind { CLASS, INTERFACE, ENUMERATION }

    public ClassifierModel {
        properties = List.copyOf(properties);
        operations = List.copyOf(operations);
        literals = List.copyOf(literals);
        generalizations = List.copyOf(generalizations);
        interfaces = List.copyOf(interfaces);
        stereotypes = List.copyOf(stereotypes);
    }

    public boolean isBlock() {
        return stereotypes.stream().anyMatch(s -> s.name().equals("Block"));
    }
}
