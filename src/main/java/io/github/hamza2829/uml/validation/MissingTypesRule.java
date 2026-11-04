package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;

public final class MissingTypesRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var findings = new ArrayList<Finding>();
        for (var e : model.elements()) {
            if (e instanceof Property p && p.type().isBlank()) findings.add(Finding.error("missing-types", p, "Property or parameter has no type"));
            if (e instanceof Operation op && op.returnType().isBlank()) findings.add(Finding.error("missing-types", op, "Return parameter has no type"));
        }
        return findings;
    }
}
