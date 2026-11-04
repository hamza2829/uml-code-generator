package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlocksWithoutPartsRuleTest extends ModelFixture {
    @Test void warnsForLeafBlocksButAllowsAssemblies() {
        var f = new BlocksWithoutPartsRule().evaluate(sample("sysml-rover"));
        assertEquals(java.util.Set.of("drive", "wheel"), f.stream().map(Finding::elementId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(f.stream().allMatch(x -> x.severity() == Finding.Severity.WARNING));
        assertFalse(RulesEngine.hasErrors(f));
    }
    @Test void ordinaryClassesNeedNoParts() { assertTrue(new BlocksWithoutPartsRule().evaluate(sample("library")).isEmpty()); }
}
