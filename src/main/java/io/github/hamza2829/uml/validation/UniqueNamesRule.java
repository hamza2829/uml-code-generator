package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;

public final class UniqueNamesRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var findings = new ArrayList<Finding>();
        for (var p : model.allPackages()) {
            var members = new ArrayList<NamedElement>(p.packages());
            members.addAll(p.classifiers());
            members.addAll(p.associations());
            check(members, "package " + p.name(), findings);
        }
        for (var c : model.classifiers()) {
            check(c.properties(), "classifier " + c.name(), findings);
            for (var op : c.operations()) check(op.parameters(), "operation " + op.name(), findings);
            var seen = new HashSet<String>();
            for (var literal : c.literals()) if (!seen.add(literal)) {
                findings.add(Finding.error("unique-names", c, "Duplicate enumeration literal " + literal));
            }
        }
        return findings;
    }

    private void check(List<? extends NamedElement> elements, String scope, List<Finding> findings) {
        var seen = new HashSet<String>();
        for (var e : elements) if (!e.name().isBlank() && !seen.add(e.name())) {
            findings.add(Finding.error("unique-names", e, "Duplicate name in " + scope));
        }
    }
}
