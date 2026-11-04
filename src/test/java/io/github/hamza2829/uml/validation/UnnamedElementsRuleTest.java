package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnnamedElementsRuleTest extends ModelFixture {
    @Test void reportsBlankNamesWithIdAndReadableFallback() {
        var f = new UnnamedElementsRule().evaluate(sample("invalid"));
        assertEquals(2, f.size());
        assertTrue(f.get(0).toString().contains("<unnamed>"));
        assertTrue(f.get(0).toString().contains("noName"));
    }
    @Test void checksEnumLiterals() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Enumeration' xmi:id='e' name='State'><ownedLiteral name=' '/></packagedElement>");
        assertEquals(1, new UnnamedElementsRule().evaluate(m).size());
    }
    @Test void namedElementsAreAccepted() { assertTrue(new UnnamedElementsRule().evaluate(sample("library")).isEmpty()); }
}
