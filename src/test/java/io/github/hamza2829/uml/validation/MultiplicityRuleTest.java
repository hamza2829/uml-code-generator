package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class MultiplicityRuleTest extends ModelFixture {
    @ParameterizedTest @CsvSource({"0,1,0", "1,1,0", "0,*,0", "4,4,0", "0,0,0", "-1,1,1", "2,1,1", "0,-2,1"})
    void enforcesBounds(String lower, String upper, int errors) throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id='p' name='value' lower='" + lower + "' upper='" + upper + "'/>"));
        assertEquals(errors, new MultiplicityRule().evaluate(m).size());
    }
    @Test void checksAssociationsAndOperationBounds() throws Exception {
        var m = parse(clazz("<ownedOperation xmi:id='op' name='run'><ownedParameter direction='return' lower='2' upper='1'/></ownedOperation>")
                + "<packagedElement xmi:type='uml:Association' xmi:id='a' name='link'><ownedEnd xmi:id='s' type='c' lower='2' upper='1'/><ownedEnd xmi:id='t' type='c' lower='-1'/></packagedElement>");
        assertEquals(3, new MultiplicityRule().evaluate(m).size());
    }
}
