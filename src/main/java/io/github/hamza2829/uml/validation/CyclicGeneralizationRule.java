package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.*;
import java.util.stream.Stream;

public final class CyclicGeneralizationRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        return model.classifiers().stream().filter(c -> reaches(model, c.id(), c.id(), new HashSet<>()))
                .map(c -> Finding.error("cyclic-generalization", c, "Generalization or realization forms an inheritance cycle")).toList();
    }

    private boolean reaches(Model m, String current, String start, Set<String> visited) {
        if (!visited.add(current)) return false;
        return m.classifier(current).map(c -> Stream.concat(c.generalizations().stream(), c.interfaces().stream())
                .anyMatch(parent -> parent.equals(start) || reaches(m, parent, start, visited))).orElse(false);
    }
}
