package io.github.hamza2829.uml.model;

public record Association(String id, String name, String documentation, String source, String target,
                          String sourceRole, String targetRole, Multiplicity sourceMultiplicity,
                          Multiplicity targetMultiplicity) implements NamedElement { }
