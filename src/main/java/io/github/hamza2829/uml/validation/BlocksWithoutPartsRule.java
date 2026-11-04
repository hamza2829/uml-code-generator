package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.*;
import java.util.List;

public final class BlocksWithoutPartsRule implements Rule {
    @Override public List<Finding> evaluate(Model model) {
        return model.classifiers().stream().filter(ClassifierModel::isBlock)
                .filter(c -> c.properties().stream().noneMatch(p -> p.role() == Property.Role.PART))
                .map(c -> new Finding(Finding.Severity.WARNING, "blocks-without-parts", c.id(), c.name(),
                        "Block has no part properties; confirm that it is a leaf block")).toList();
    }
}
