package io.github.hamza2829.uml.transformation;

import io.github.hamza2829.uml.*;
import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.generation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SysmlToUmlTest extends ModelFixture {
    @Test void matchesBeforeAndAfterDiagrams() throws Exception {
        var before = sample("sysml-rover");
        var after = new SysmlToUml().transform(before);
        assertEquals(snapshot("rover-before.puml"), new PlantUmlGenerator().render(before));
        assertEquals(snapshot("rover-after.puml"), new PlantUmlGenerator().render(after));
    }

    @Test void convertsPartsToAssociationsAndValuesToAttributesWithoutMutatingSource() {
        var before = sample("sysml-rover");
        var after = new SysmlToUml().transform(before);
        assertEquals(1, after.allPackages().size());
        assertEquals("flat", after.packages().get(0).name());
        assertTrue(after.classifiers().stream().noneMatch(ClassifierModel::isBlock));
        var rover = after.classifier("rover").orElseThrow();
        assertEquals(List.of(new Property("roverLabel", "label", "", "String", Multiplicity.ONE, Property.Role.ATTRIBUTE)), rover.properties());
        assertEquals(List.of(
                new Association("drivePart_association", "Rover_drive", "", "rover", "drive", "", "drive", Multiplicity.ONE, Multiplicity.ONE),
                new Association("wheelParts_association", "Rover_wheels", "", "rover", "wheel", "", "wheels", Multiplicity.ONE, new Multiplicity(4, 4))), after.associations());
        assertEquals(before.classifier("rover").orElseThrow().documentation(), rover.documentation());
        assertTrue(before.classifier("rover").orElseThrow().isBlock());
        assertEquals(3, before.classifier("rover").orElseThrow().properties().size());
        assertTrue(before.associations().isEmpty());
    }

    @Test void preservesUmlRelationshipsOperationsAndLiterals() {
        var before = sample("sensor-tracker");
        var after = new SysmlToUml().transform(before);
        assertEquals(before.classifier("radar").orElseThrow().generalizations(), after.classifier("radar").orElseThrow().generalizations());
        assertEquals(before.classifier("feed").orElseThrow().operations(), after.classifier("feed").orElseThrow().operations());
        assertEquals(before.classifier("state").orElseThrow().literals(), after.classifier("state").orElseThrow().literals());
        assertTrue(after.associations().containsAll(before.associations()));
        assertEquals(3, after.associations().size());
    }

    @Test void transformationIsDeterministicAndIdempotent() {
        var source = sample("sysml-rover");
        var transformation = new SysmlToUml();
        var once = transformation.transform(source);
        assertEquals(once, transformation.transform(source));
        assertEquals(once, transformation.transform(once));
    }

    @Test void resolvesNamesWhenFlatteningPackages() throws Exception {
        var source = parse("<packagedElement xmi:type='uml:Package' xmi:id='a' name='one'>" + clazz("") + "</packagedElement>"
                + "<packagedElement xmi:type='uml:Package' xmi:id='b' name='two'><packagedElement xmi:type='uml:Class' xmi:id='d' name='Device'/></packagedElement>"
                + "<packagedElement xmi:type='uml:Class' xmi:id='e' name='Flat_example_one_Device'/>");
        var after = new SysmlToUml().transform(source);
        assertEquals(3, after.classifiers().stream().map(ClassifierModel::name).distinct().count());
        try (var loader = Compilation.compile(new JavaGenerator().generate(after, temp.resolve("java")), temp.resolve("classes"))) {
            assertNotNull(loader.loadClass("flat.Flat_example_one_Device_2"));
        }
    }

    private String snapshot(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/expected/" + name)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
