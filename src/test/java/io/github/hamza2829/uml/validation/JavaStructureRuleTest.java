package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.ModelFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class JavaStructureRuleTest extends ModelFixture {
    @ParameterizedTest @CsvSource({"Interface,Class", "Class,Enumeration", "Enumeration,Interface"})
    void rejectsUnrepresentableInheritance(String child, String parent) throws Exception {
        var m = parse("<packagedElement xmi:type='uml:" + parent + "' xmi:id='p' name='Parent'/>"
                + "<packagedElement xmi:type='uml:" + child + "' xmi:id='c' name='Child'><generalization general='p'/></packagedElement>");
        assertEquals(1, new JavaStructureRule().evaluate(m).size());
    }
    @Test void rejectsMultipleClassParentsAndInvalidRealization() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Class' xmi:id='a' name='A'/>"
                + "<packagedElement xmi:type='uml:Class' xmi:id='b' name='B'/>"
                + clazz("<generalization general='a'/><generalization general='b'/><interfaceRealization contract='a'/>"));
        assertEquals(2, new JavaStructureRule().evaluate(m).size());
    }
    @Test void rejectsErasedDuplicateSignaturesAndFinalMethods() throws Exception {
        var m = parse(clazz("<ownedOperation xmi:id='a' name='run'><ownedParameter xmi:id='p1' name='items' lower='0' upper='*'>" + primitive("String") + "</ownedParameter></ownedOperation>"
                + "<ownedOperation xmi:id='b' name='run'><ownedParameter xmi:id='p2' name='numbers' lower='0' upper='*'>" + primitive("Integer") + "</ownedParameter></ownedOperation>"
                + "<ownedOperation xmi:id='w' name='wait'/><ownedAttribute xmi:id='classField' name='class'/>"));
        assertEquals(3, new JavaStructureRule().evaluate(m).size());
    }
    @Test void rejectsPrimitivePartProperties() throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id='p' name='part' aggregation='composite'>" + primitive("Integer") + "</ownedAttribute>"));
        assertEquals(1, new JavaStructureRule().evaluate(m).size());
    }
    @Test void acceptsValidSamples() {
        for (var name : java.util.List.of("library", "sysml-rover", "sensor-tracker", "contracts")) {
            assertTrue(new JavaStructureRule().evaluate(sample(name)).isEmpty(), name);
        }
    }
}
