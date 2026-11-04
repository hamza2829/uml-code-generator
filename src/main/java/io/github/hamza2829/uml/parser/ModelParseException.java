package io.github.hamza2829.uml.parser;

public class ModelParseException extends RuntimeException {
    public ModelParseException(String file, String id, String message) {
        super(file + " [XMI id=" + id + "]: " + message);
    }
}
