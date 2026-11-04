package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;

public final class MultiplicityRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        var findings = new ArrayList<Finding>();
        for (var e : model.elements()) {
            if (e instanceof Property p) check(p, p.multiplicity(), findings);
            if (e instanceof Operation op) check(op, op.returnMultiplicity(), findings);
            if (e instanceof Association a) {
                check(a, a.sourceMultiplicity(), findings);
                check(a, a.targetMultiplicity(), findings);
            }
        }
        return findings;
    }

    private void check(NamedElement e, Multiplicity m, List<Finding> findings) {
        if (!m.isValid()) findings.add(Finding.error("multiplicities", e, "Invalid multiplicity " + m + "; require 0 <= lower <= upper, or upper=*"));
    }
}
