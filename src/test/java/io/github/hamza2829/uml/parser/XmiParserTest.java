package io.github.hamza2829.uml.parser;

import io.github.hamza2829.uml.ModelFixture;
import io.github.hamza2829.uml.model.*;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class XmiParserTest extends ModelFixture {
    @Test void readsNestedPackagesDocumentationAndAssociations() {
        var m = sample("library");
        assertEquals(2, m.allPackages().size());
        assertEquals(3, m.classifiers().size());
        var book = m.classifier("book").orElseThrow();
        assertEquals("A catalog entry. Synthetic example data only.", book.documentation());
        assertEquals("Change the display title.", book.operations().get(0).documentation());
        assertEquals("void", book.operations().get(0).returnType());
        assertEquals("newTitle", book.operations().get(0).parameters().get(0).name());
        assertEquals("0..*", m.associations().get(0).targetMultiplicity().toString());
        assertEquals("reader", m.associations().get(0).source());
        assertEquals(ClassifierModel.Kind.ENUMERATION, m.classifier("genre").orElseThrow().kind());
    }

    @Test void readsBlocksPartsAndValues() {
        var m = sample("sysml-rover");
        var rover = m.classifier("rover").orElseThrow();
        assertTrue(rover.isBlock());
        assertEquals(Property.Role.VALUE, rover.properties().get(0).role());
        assertEquals(Property.Role.PART, rover.properties().get(1).role());
        assertEquals(new Multiplicity(4, 4), rover.properties().get(2).multiplicity());
    }

    @Test void readsForwardReferencesLocalPrimitivesAndRealizations() {
        var m = sample("sensor-tracker");
        assertTrue(m.classifier("baseSensor").orElseThrow().isAbstract());
        var radar = m.classifier("radar").orElseThrow();
        assertEquals(java.util.List.of("baseSensor"), radar.generalizations());
        assertEquals(java.util.List.of("feed"), radar.interfaces());
        assertEquals("String", radar.properties().get(0).type());
        assertTrue(m.classifier("feed").orElseThrow().operations().get(0).returnMultiplicity().isMany());
    }

    @Test void readsClassOwnedAssociationEndsAndIdrefTypes() {
        var m = sample("contracts");
        assertEquals("String", m.classifier("named").orElseThrow().properties().get(0).type());
        assertEquals("peer", m.associations().get(0).source());
        assertEquals("serviceImpl", m.associations().get(0).target());
    }

    @ParameterizedTest @ValueSource(strings = {"String", "Integer", "Real", "Boolean", "UnlimitedNatural"})
    void readsPrimitiveLibraryTypes(String type) throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id=\"p\" name=\"value\">" + primitive(type) + "</ownedAttribute>"));
        assertEquals(type, m.classifier("c").orElseThrow().properties().get(0).type());
    }

    @ParameterizedTest @CsvSource({"0,1,false", "1,1,false", "0,*,true", "2,5,true", "-1,1,false", "2,1,false"})
    void preservesBoundsForValidation(String lower, String upper, boolean many) throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id=\"p\" name=\"value\" lower=\"" + lower + "\" upper=\"" + upper + "\">" + primitive("Integer") + "</ownedAttribute>"));
        var bounds = m.classifier("c").orElseThrow().properties().get(0).multiplicity();
        assertEquals(lower + ".." + upper, bounds.toString());
        assertEquals(many, bounds.isMany());
    }

    @Test void omittedLiteralValuesMeanZero() throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id=\"p\" name=\"value\"><lowerValue/><upperValue/>" + primitive("Integer") + "</ownedAttribute>"));
        assertEquals(new Multiplicity(0, 0), m.classifier("c").orElseThrow().properties().get(0).multiplicity());
    }

    static Stream<Arguments> badElements() {
        return Stream.of(
            Arguments.of("<packagedElement xmi:type=\"uml:Actor\" xmi:id=\"bad\"/>", "bad", "Unknown element type"),
            Arguments.of("<packagedElement xmi:type=\"uml:Class\" name=\"Device\"/>", "<missing>", "Missing XMI id"),
            Arguments.of(clazz("<ownedAttribute xmi:id=\"p\" name=\"value\" type=\"missing\"/>"), "p", "Missing classifier reference"),
            Arguments.of(clazz("<generalization general=\"missing\"/>"), "c", "Missing classifier reference"),
            Arguments.of(clazz("<interfaceRealization contract=\"missing\"/>"), "c", "Missing classifier reference"),
            Arguments.of(clazz("<ownedAttribute xmi:id=\"p\" lower=\"oops\"/>"), "p", "Invalid lower multiplicity"),
            Arguments.of(clazz("<ownedAttribute xmi:id=\"p\" upper=\"oops\"/>"), "p", "Invalid upper multiplicity"),
            Arguments.of(clazz("<ownedAttribute xmi:id=\"p\"><type href=\"other.uml#Device\"/></ownedAttribute>"), "p", "Unsupported external type"),
            Arguments.of(clazz("<ownedAttribute xmi:id=\"p\"/><ownedAttribute xmi:id=\"p\"/>"), "p", "Duplicate XMI id"),
            Arguments.of(clazz("<nestedClassifier xmi:id=\"nested\"/>"), "nested", "Unknown classifier element"),
            Arguments.of(clazz("<ownedOperation xmi:id=\"op\"><ownedParameter xmi:id=\"p\" direction=\"out\"/></ownedOperation>"), "p", "Unsupported parameter direction"),
            Arguments.of(clazz("<ownedOperation xmi:id=\"op\"><ownedParameter direction=\"return\"/><ownedParameter xmi:id=\"p\" direction=\"return\"/></ownedOperation>"), "p", "Multiple return parameters"),
            Arguments.of("<packagedElement xmi:type=\"uml:PrimitiveType\" xmi:id=\"p\" name=\"Unknown\"/>", "p", "Unknown primitive type"),
            Arguments.of("<packagedElement xmi:type=\"uml:Association\" xmi:id=\"a\" memberEnd=\"lost other\"/>", "a", "Missing reference"),
            Arguments.of("<packagedElement xmi:type=\"uml:Association\" xmi:id=\"a\"/>", "a", "Only binary associations"),
            Arguments.of("<sysml:Block xmi:id=\"block\" base_Class=\"lost\"/>", "block", "Block reference"),
            Arguments.of("<packagedElement xmi:type=\"uml:Interface\" xmi:id=\"i\"/><sysml:Block xmi:id=\"block\" base_Class=\"i\"/>", "block", "Block reference"),
            Arguments.of(clazz("<ownedOperation xmi:id=\"op\"><ownedParameter direction=\"return\" type=\"lost\"/></ownedOperation>"), "op", "Missing classifier reference"),
            Arguments.of(clazz("<ownedOperation xmi:id=\"op\"><ownedParameter xmi:id=\"p\" type=\"lost\"/></ownedOperation>"), "p", "Missing classifier reference"),
            Arguments.of("<packagedElement xmi:type=\"uml:Association\" xmi:id=\"a\"><ownedEnd xmi:id=\"e1\" type=\"lost\"/><ownedEnd xmi:id=\"e2\" type=\"lost\"/></packagedElement>", "a", "Missing classifier reference")
        );
    }

    @ParameterizedTest @MethodSource("badElements")
    void failuresIncludeFileIdAndCause(String body, String id, String cause) {
        var e = assertThrows(ModelParseException.class, () -> parse(body));
        assertTrue(e.getMessage().contains("fixture.xmi [XMI id=" + id + "]"), e.getMessage());
        assertTrue(e.getMessage().contains(cause), e.getMessage());
    }

    @ParameterizedTest @ValueSource(strings = {"<broken>", "<root/>", "<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><a>&x;</a>", "<root xmlns:u='urn:uml'><u:Model/><u:Model/></root>"})
    void rejectsBrokenOrUnsafeDocuments(String xml) {
        var e = assertThrows(ModelParseException.class, () -> parseDocument(xml));
        assertTrue(e.getMessage().contains("fixture.xmi"));
        assertTrue(e.getMessage().contains("XMI id="));
    }

    @Test void missingFileIsContextual() {
        var e = assertThrows(ModelParseException.class, () -> new XmiParser().parse(temp.resolve("absent.xmi")));
        assertTrue(e.getMessage().contains("absent.xmi [XMI id=<document>]"));
    }

    @Test void returnedCollectionsCannotBeMutated() {
        var model = sample("library");
        assertThrows(UnsupportedOperationException.class, () -> model.packages().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.classifier("book").orElseThrow().properties().clear());
    }
}
