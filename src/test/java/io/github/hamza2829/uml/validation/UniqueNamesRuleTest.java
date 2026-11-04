package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UniqueNamesRuleTest extends ModelFixture {
    @Test void duplicateClassifierNamesAreErrors() {
        var f = new UniqueNamesRule().evaluate(sample("invalid"));
        assertEquals(1, f.size());
        assertEquals("loop", f.get(0).elementId());
        assertEquals(Finding.Severity.ERROR, f.get(0).severity());
    }
    @Test void sameNamesInSeparatePackagesAreAllowed() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Package' xmi:id='p1' name='one'>" + clazz("") + "</packagedElement>"
                + "<packagedElement xmi:type='uml:Package' xmi:id='p2' name='two'><packagedElement xmi:type='uml:Class' xmi:id='c2' name='Device'/></packagedElement>");
        assertTrue(new UniqueNamesRule().evaluate(m).isEmpty());
    }
    @Test void duplicatesInPropertiesParametersAndEnumLiteralsAreReported() throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id='a' name='value'/><ownedAttribute xmi:id='b' name='value'/>"
                + "<ownedOperation xmi:id='op' name='run'><ownedParameter xmi:id='p1' name='input'/><ownedParameter xmi:id='p2' name='input'/></ownedOperation>")
                + "<packagedElement xmi:type='uml:Enumeration' xmi:id='e' name='State'><ownedLiteral name='NEW'/><ownedLiteral name='NEW'/></packagedElement>");
        assertEquals(3, new UniqueNamesRule().evaluate(m).size());
    }
}
