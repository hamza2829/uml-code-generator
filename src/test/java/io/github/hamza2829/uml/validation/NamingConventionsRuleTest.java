package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NamingConventionsRuleTest extends ModelFixture {
    @Test void reportsWrongCase() { assertEquals(3, new NamingConventionsRule().evaluate(sample("invalid")).size()); }
    @ParameterizedTest @ValueSource(strings = {"class", "record", "var", "bad-name", "../escape", "1thing"})
    void rejectsInvalidIdentifiers(String name) { assertFalse(NamingConventionsRule.identifier(name)); }
    @Test void checksOperationsEnumLiteralsAndRoles() throws Exception {
        var m = parse(clazz("<ownedOperation xmi:id='op' name='Run'/>")
                + "<packagedElement xmi:type='uml:Enumeration' xmi:id='e' name='State'><ownedLiteral name='class'/></packagedElement>"
                + "<packagedElement xmi:type='uml:Association' xmi:id='a' name='link'><ownedEnd xmi:id='s' type='c' name='Bad'/><ownedEnd xmi:id='t' type='c' name='good'/></packagedElement>");
        assertEquals(3, new NamingConventionsRule().evaluate(m).size());
    }
    @Test void acceptsSampleNames() { assertTrue(new NamingConventionsRule().evaluate(sample("sensor-tracker")).isEmpty()); }
}
