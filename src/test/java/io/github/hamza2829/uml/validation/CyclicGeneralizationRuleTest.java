package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CyclicGeneralizationRuleTest extends ModelFixture {
    @Test void reportsEachParticipantInCycle() {
        assertEquals(java.util.Set.of("bad", "loop"), new CyclicGeneralizationRule().evaluate(sample("invalid")).stream().map(Finding::elementId).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void detectsSelfCycle() throws Exception {
        assertEquals(1, new CyclicGeneralizationRule().evaluate(parse(clazz("<generalization general='c'/>"))).size());
    }
    @Test void diamondIsNotACycle() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Interface' xmi:id='a' name='A'/>"
                + "<packagedElement xmi:type='uml:Interface' xmi:id='b' name='B'><generalization general='a'/></packagedElement>"
                + "<packagedElement xmi:type='uml:Interface' xmi:id='c' name='C'><generalization general='a'/></packagedElement>"
                + "<packagedElement xmi:type='uml:Interface' xmi:id='d' name='D'><generalization general='b'/><generalization general='c'/></packagedElement>");
        assertTrue(new CyclicGeneralizationRule().evaluate(m).isEmpty());
    }
    @Test void validInheritanceHasNoFindings() { assertTrue(new CyclicGeneralizationRule().evaluate(sample("sensor-tracker")).isEmpty()); }
}
