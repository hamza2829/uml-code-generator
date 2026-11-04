package io.github.hamza2829.uml;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.parser.XmiParser;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.io.TempDir;

public abstract class ModelFixture {
    @TempDir protected Path temp;

    protected Model parse(String body) throws IOException {
        return parseDocument("""
                <xmi:XMI xmlns:xmi="http://www.omg.org/spec/XMI/20131001" xmlns:uml="http://www.eclipse.org/uml2/5.0.0/UML" xmlns:sysml="http://www.omg.org/spec/SysML/20100301/SysML-profile">
                  <uml:Model xmi:id="model" name="example">
                """ + body + "</uml:Model></xmi:XMI>");
    }

    protected Model parseDocument(String xml) throws IOException {
        Path path = temp.resolve("fixture.xmi");
        Files.writeString(path, xml);
        return new XmiParser().parse(path);
    }

    protected Model sample(String name) { return new XmiParser().parse(Path.of("models", name + ".xmi")); }

    protected static String clazz(String body) {
        return "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c\" name=\"Device\">" + body + "</packagedElement>";
    }

    protected static String primitive(String type) {
        return "<type href=\"pathmap://UML_LIBRARIES/UMLPrimitiveTypes.library.uml#" + type + "\"/>";
    }
}
