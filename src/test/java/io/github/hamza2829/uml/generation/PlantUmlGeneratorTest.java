package io.github.hamza2829.uml.generation;

import io.github.hamza2829.uml.ModelFixture;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlantUmlGeneratorTest extends ModelFixture {
    @Test void includesEnumsInterfacesInheritanceAndOperations() {
        String diagram = new PlantUmlGenerator().render(sample("sensor-tracker"));
        assertTrue(diagram.contains("abstract class \"aeris.sensing.BaseSensor\""));
        assertTrue(diagram.contains("interface \"aeris.sensing.SensorFeed\""));
        assertTrue(diagram.contains("enum \"aeris.tracking.TrackState\""));
        assertTrue(diagram.contains(" --|> "));
        assertTrue(diagram.contains(" ..|> "));
        assertTrue(diagram.contains("accept(track : aeris.tracking.TrackRecord) : void"));
        assertTrue(diagram.contains("ACTIVE\n"));
    }
    @Test void writesUtf8DiagramAndCreatesDirectories() throws Exception {
        var file = temp.resolve("nested/model.puml");
        var generator = new PlantUmlGenerator();
        generator.generate(sample("library"), file);
        assertEquals(generator.render(sample("library")), Files.readString(file));
    }
    @Test void escapesQuotedLabels() throws Exception {
        var m = parse(clazz("") + "<packagedElement xmi:type='uml:Association' xmi:id='a' name='a&quot;b&#10;c\\d'><ownedEnd xmi:id='s' type='c' name='left'/><ownedEnd xmi:id='t' type='c' name='right'/></packagedElement>");
        assertTrue(new PlantUmlGenerator().render(m).contains("a\\\"b\\nc\\\\d"));
    }
}
