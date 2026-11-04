package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RulesEngineTest extends ModelFixture {
    @Test void returnsEveryExpectedFindingWithoutStoppingAtFirstError() {
        var f = new RulesEngine().validate(sample("invalid"));
        assertEquals(11, f.size());
        assertEquals(10, f.stream().filter(x -> x.severity() == Finding.Severity.ERROR).count());
        assertEquals(java.util.Set.of("unique-names", "missing-types", "multiplicities", "cyclic-generalization", "unnamed-elements", "naming-conventions", "blocks-without-parts"),
                f.stream().map(Finding::rule).collect(java.util.stream.Collectors.toSet()));
        assertTrue(RulesEngine.hasErrors(f));
        assertTrue(f.stream().allMatch(x -> !x.message().isBlank() && !x.elementId().isBlank()));
    }
    @Test void supportsSelectingRules() {
        var f = new RulesEngine(java.util.List.of(new MissingTypesRule())).validate(sample("invalid"));
        assertEquals(1, f.size());
        assertTrue(f.get(0).toString().contains("Upper [XMI id=missingType]"));
    }
}
