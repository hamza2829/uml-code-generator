package io.github.hamza2829.uml.model;

public record Multiplicity(int lower, int upper) {
    public static final Multiplicity ONE = new Multiplicity(1, 1);

    public boolean isMany() {
        return upper == -1 || upper > 1;
    }

    public boolean isValid() {
        return lower >= 0 && (upper == -1 || upper >= lower);
    }

    @Override public String toString() {
        return lower + ".." + (upper == -1 ? "*" : upper);
    }
}
