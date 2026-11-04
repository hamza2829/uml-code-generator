package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.NamedElement;

public record Finding(Severity severity, String rule, String elementId, String elementName, String message) {
    public enum Severity { ERROR, WARNING }

    public static Finding error(String rule, NamedElement e, String message) {
        return new Finding(Severity.ERROR, rule, e.id(), e.name(), message);
    }

    @Override public String toString() {
        return severity + " [" + rule + "] " + (elementName.isBlank() ? "<unnamed>" : elementName)
                + " [XMI id=" + elementId + "]: " + message;
    }
}
