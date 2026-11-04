package io.github.hamza2829.uml.model;

public record Property(String id, String name, String documentation, String type,
                       Multiplicity multiplicity, Role role) implements NamedElement {
    public enum Role { ATTRIBUTE, VALUE, PART }
}
