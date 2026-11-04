package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.model.Package;
import java.util.*;
import javax.lang.model.SourceVersion;

public final class NamingConventionsRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var result = new ArrayList<Finding>();
        for (var e : model.elements()) {
            if (e.name().isBlank()) continue;
            if (e instanceof ClassifierModel && (!identifier(e.name()) || !e.name().matches("[A-Z][A-Za-z0-9_]*"))) {
                result.add(Finding.error("naming-conventions", e, "Classifier name must be a Java identifier starting with an uppercase letter"));
            } else if ((e instanceof Property || e instanceof Operation || e instanceof Package)
                    && (!identifier(e.name()) || !e.name().matches("[a-z][A-Za-z0-9_]*"))) {
                result.add(Finding.error("naming-conventions", e, "Name must be a Java identifier starting with a lowercase letter"));
            }
        }
        for (var c : model.classifiers()) for (var literal : c.literals()) {
            if (!identifier(literal)) result.add(Finding.error("naming-conventions", c, "Invalid Java enum literal " + literal));
        }
        for (var a : model.associations()) for (var role : List.of(a.sourceRole(), a.targetRole())) {
            if (!role.isEmpty() && (!identifier(role) || !role.matches("[a-z][A-Za-z0-9_]*"))) {
                result.add(Finding.error("naming-conventions", a, "Invalid association role name " + role));
            }
        }
        return result;
    }

    public static boolean identifier(String name) {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name)
                && !Set.of("var", "yield", "record", "sealed", "permits").contains(name);
    }
}
