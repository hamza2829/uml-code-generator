package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MissingTypesRuleTest extends ModelFixture {
    @Test void reportsMissingAttributeType() {
        var f = new MissingTypesRule().evaluate(sample("invalid"));
        assertEquals("missingType", f.get(0).elementId());
        assertEquals(1, f.size());
    }
    @Test void checksInputAndReturnParameters() throws Exception {
        var m = parse(clazz("<ownedOperation xmi:id='op' name='run'><ownedParameter xmi:id='p' name='input'/><ownedParameter direction='return'/></ownedOperation>"));
        assertEquals(2, new MissingTypesRule().evaluate(m).size());
    }
    @Test void voidOperationsNeedNoReturnType() {
        assertTrue(new MissingTypesRule().evaluate(sample("library")).isEmpty());
    }
}
