package io.github.hamza2829.uml.model;

import java.util.List;

public record Operation(String id, String name, String documentation, String returnType,
                        Multiplicity returnMultiplicity, List<Property> parameters) implements NamedElement {
    public Operation { parameters = List.copyOf(parameters); }
}
