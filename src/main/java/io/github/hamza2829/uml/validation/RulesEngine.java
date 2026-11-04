package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.Model;
import java.util.List;

public final class RulesEngine {
    private final List<Rule> rules;

    public RulesEngine() {
        this(List.of(new UniqueNamesRule(), new MissingTypesRule(), new MultiplicityRule(),
                new CyclicGeneralizationRule(), new UnnamedElementsRule(), new NamingConventionsRule(),
                new BlocksWithoutPartsRule(), new JavaStructureRule()));
    }

    public RulesEngine(List<Rule> rules) { this.rules = List.copyOf(rules); }

    public List<Finding> validate(Model model) {
        return rules.stream().flatMap(r -> r.evaluate(model).stream()).toList();
    }

    public static boolean hasErrors(List<Finding> findings) {
        return findings.stream().anyMatch(f -> f.severity() == Finding.Severity.ERROR);
    }
}
