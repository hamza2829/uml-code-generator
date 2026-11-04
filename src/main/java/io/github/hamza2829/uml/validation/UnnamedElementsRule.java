package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;

public final class UnnamedElementsRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var result = new ArrayList<Finding>();
        for (var e : model.elements()) if (e.name().isBlank()) result.add(Finding.error("unnamed-elements", e, "Element must have a name"));
        for (var c : model.classifiers()) if (c.literals().stream().anyMatch(String::isBlank)) {
            result.add(Finding.error("unnamed-elements", c, "Enumeration has an unnamed literal"));
        }
        return result;
    }
}
