package io.github.hamza2829.uml.generation;

import io.github.hamza2829.uml.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class JavaGeneratorTest extends ModelFixture {
    @Test void generatedGettersSettersListsAndEnumsWork() throws Exception {
        var sources = new JavaGenerator().generate(sample("library"), temp.resolve("java"));
        try (var classes = Compilation.compile(sources, temp.resolve("classes"))) {
            var type = classes.loadClass("library.catalog.Book");
            var book = type.getConstructor().newInstance();
            type.getMethod("setTitle", String.class).invoke(book, "A synthetic title");
            assertEquals("A synthetic title", type.getMethod("getTitle").invoke(book));
            assertEquals(List.of(), type.getMethod("getTags").invoke(book));
            type.getMethod("setTags", List.class).invoke(book, List.of("catalog"));
            assertEquals(List.of("catalog"), type.getMethod("getTags").invoke(book));
            assertEquals(2, classes.loadClass("library.catalog.Genre").getEnumConstants().length);
            var e = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> type.getMethod("rename", String.class).invoke(book, "other"));
            assertInstanceOf(UnsupportedOperationException.class, e.getCause());
        }
        String book = Files.readString(temp.resolve("java/library/catalog/Book.java"));
        assertTrue(book.contains("A catalog entry. Synthetic example data only."));
        assertTrue(book.contains("Change the display title."));
        assertTrue(book.contains("java.util.List<java.lang.String> tags"));
        assertTrue(book.contains("java.util.List<library.Reader> readers"));
    }

    @Test void generatedInterfacesAndInheritedContractsWork() throws Exception {
        var sources = new JavaGenerator().generate(sample("contracts"), temp.resolve("java"));
        try (var classes = Compilation.compile(sources, temp.resolve("classes"))) {
            var service = classes.loadClass("contracts.LocalService");
            assertTrue(classes.loadClass("contracts.Service").isAssignableFrom(service));
            assertTrue(classes.loadClass("contracts.Named").isAssignableFrom(service));
            var instance = service.getConstructor().newInstance();
            service.getMethod("setName", String.class).invoke(instance, "local");
            assertEquals("local", service.getMethod("getName").invoke(instance));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"String", "Integer", "Real", "Boolean", "UnlimitedNatural"})
    void primitiveTypesAndListOperationSignaturesCompile(String type) throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id='v' name='value'>" + primitive(type) + "</ownedAttribute>"
                + "<ownedOperation xmi:id='op' name='convert'><ownedParameter xmi:id='p' name='inputs' lower='0' upper='*'>" + primitive(type)
                + "</ownedParameter><ownedParameter direction='return' lower='0' upper='*'>" + primitive(type) + "</ownedParameter></ownedOperation>"));
        try (var loader = Compilation.compile(new JavaGenerator().generate(m, temp.resolve("java")), temp.resolve("classes"))) {
            assertEquals(List.class, loader.loadClass("example.Device").getMethod("convert", List.class).getReturnType());
        }
    }

    @Test void hostileDocumentationIsEscapedAndCompiles() throws Exception {
        var m = parse(clazz("<ownedComment body='First &amp; &lt;tag&gt; */ @param {{NAME}}&#10;Second line&#10;\\u000a dangerous'/>"));
        var sources = new JavaGenerator().generate(m, temp.resolve("java"));
        String java = Files.readString(sources.get(0));
        assertTrue(java.contains("&amp; &lt;tag&gt; *&#47; &#64;param {{NAME}}"));
        assertTrue(java.contains("&#92;u000a"));
        try (var loader = Compilation.compile(sources, temp.resolve("classes"))) { assertNotNull(loader.loadClass("example.Device")); }
    }

    @Test void invalidModelCreatesNoOutput() {
        Path output = temp.resolve("out");
        assertThrows(GenerationException.class, () -> new JavaGenerator().generate(sample("invalid"), output));
        assertFalse(Files.exists(output));
    }

    @Test void emptyEnumAndAbstractClassCompile() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Enumeration' xmi:id='e' name='Empty'/>"
                + "<packagedElement xmi:type='uml:Class' xmi:id='a' name='AbstractDevice' isAbstract='true'/>"
                + clazz("<generalization general='a'/>"));
        try (var classes = Compilation.compile(new JavaGenerator().generate(m, temp.resolve("java")), temp.resolve("classes"))) {
            assertEquals(0, classes.loadClass("example.Empty").getEnumConstants().length);
            assertEquals("example.AbstractDevice", classes.loadClass("example.Device").getSuperclass().getName());
        }
    }

    @Test void propertyOperationCollisionIsClear() throws Exception {
        var m = parse(clazz("<ownedAttribute xmi:id='p' name='value'>" + primitive("String") + "</ownedAttribute><ownedOperation xmi:id='op' name='getValue'/>"));
        assertTrue(assertThrows(GenerationException.class, () -> new JavaGenerator().generate(m, temp)).getMessage().contains("XMI id=op"));
    }

    @Test void incompatibleInheritedPropertiesAreRejected() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Interface' xmi:id='i' name='Named'><ownedAttribute xmi:id='n' name='name'>" + primitive("String") + "</ownedAttribute></packagedElement>"
                + clazz("<interfaceRealization contract='i'/><ownedAttribute xmi:id='p' name='name'>" + primitive("Integer") + "</ownedAttribute>"));
        assertTrue(assertThrows(GenerationException.class, () -> new JavaGenerator().generate(m, temp)).getMessage().contains("Conflicting property"));
    }

    @Test void incompatibleInheritedReturnsAreRejected() throws Exception {
        var m = parse("<packagedElement xmi:type='uml:Interface' xmi:id='i' name='Named'><ownedOperation xmi:id='n' name='value'><ownedParameter direction='return'>" + primitive("String") + "</ownedParameter></ownedOperation></packagedElement>"
                + clazz("<interfaceRealization contract='i'/><ownedOperation xmi:id='op' name='value'><ownedParameter direction='return'>" + primitive("Integer") + "</ownedParameter></ownedOperation>"));
        assertTrue(assertThrows(GenerationException.class, () -> new JavaGenerator().generate(m, temp)).getMessage().contains("Conflicting inherited operation"));
    }

    @Test void outputIoFailurePropagates() throws Exception {
        Path output = Files.writeString(temp.resolve("file"), "occupied");
        assertThrows(java.io.IOException.class, () -> new JavaGenerator().generate(sample("library"), output));
    }
}
