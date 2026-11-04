package io.github.hamza2829.uml.parser;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.model.Package;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Namespace aware DOM reader for the documented structural UML XMI subset. */
public final class XmiParser {
    public Model parse(Path file) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException e) throws SAXException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            return new Reader(file.toString(), builder.parse(file.toFile())).read();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ModelParseException(file.toString(), "<document>", "Cannot read XML: " + e.getMessage());
        }
    }

    private static final class Reader {
        private final String file;
        private final Document document;
        private final Map<String, Element> ids = new LinkedHashMap<>();
        private final Map<String, List<Stereotype>> stereotypes = new HashMap<>();
        private final Map<String, String> primitiveIds = new HashMap<>();

        Reader(String file, Document document) { this.file = file; this.document = document; }

        Model read() {
            var nodes = document.getElementsByTagName("*");
            Element root = null;
            for (int i = 0; i < nodes.getLength(); i++) {
                var e = (Element) nodes.item(i);
                var id = xmi(e, "id");
                if (!id.isEmpty() && ids.putIfAbsent(id, e) != null) fail(e, "Duplicate XMI id " + id);
                if (local(e).equals("Model")) {
                    if (root != null) fail(e, "Multiple UML models are not supported");
                    root = e;
                }
                if (kind(e).equals("PrimitiveType")) {
                    if (!Model.PRIMITIVES.contains(e.getAttribute("name"))) fail(e, "Unknown primitive type");
                    primitiveIds.put(requiredId(e), e.getAttribute("name"));
                }
                if (local(e).equals("Block") && e.hasAttribute("base_Class")) {
                    stereotypes.computeIfAbsent(e.getAttribute("base_Class"), k -> new ArrayList<>())
                            .add(new Stereotype(requiredId(e), "Block"));
                }
            }
            if (root == null) throw new ModelParseException(file, "<document>", "Missing UML Model element");
            for (var base : stereotypes.keySet()) {
                var e = ids.get(base);
                if (e == null || !kind(e).equals("Class")) {
                    throw new ModelParseException(file, stereotypes.get(base).get(0).id(), "Missing or non-class Block reference " + base);
                }
            }
            var model = new Model(file, List.of(readPackage(root)));
            checkReferences(model);
            return model;
        }

        private Package readPackage(Element e) {
            var packages = new ArrayList<Package>();
            var classifiers = new ArrayList<ClassifierModel>();
            var associations = new ArrayList<Association>();
            for (var child : children(e, "packagedElement")) {
                switch (kind(child)) {
                    case "Package" -> packages.add(readPackage(child));
                    case "Class", "Interface", "Enumeration" -> classifiers.add(readClassifier(child));
                    case "Association" -> associations.add(readAssociation(child));
                    case "PrimitiveType" -> { /* Resolved through the primitive id table. */ }
                    default -> fail(child, "Unknown element type " + kind(child));
                }
            }
            return new Package(requiredId(e), e.getAttribute("name"), documentation(e), packages, classifiers, associations);
        }

        private ClassifierModel readClassifier(Element e) {
            var props = new ArrayList<Property>();
            var ops = new ArrayList<Operation>();
            var literals = new ArrayList<String>();
            var parents = new ArrayList<String>();
            var interfaces = new ArrayList<String>();
            boolean block = stereotypes.containsKey(requiredId(e));
            for (var child : children(e, null)) {
                switch (local(child)) {
                    case "ownedAttribute" -> props.add(property(child, block));
                    case "ownedOperation" -> ops.add(operation(child));
                    case "ownedLiteral" -> literals.add(child.getAttribute("name"));
                    case "generalization" -> parents.add(child.getAttribute("general"));
                    case "interfaceRealization" -> interfaces.add(child.getAttribute("contract"));
                    case "ownedComment", "eAnnotations" -> { }
                    default -> fail(child, "Unknown classifier element " + local(child));
                }
            }
            var kind = switch (kind(e)) {
                case "Interface" -> ClassifierModel.Kind.INTERFACE;
                case "Enumeration" -> ClassifierModel.Kind.ENUMERATION;
                default -> ClassifierModel.Kind.CLASS;
            };
            return new ClassifierModel(requiredId(e), e.getAttribute("name"), documentation(e), kind,
                    props, ops, literals, parents, interfaces, stereotypes.getOrDefault(requiredId(e), List.of()),
                    Boolean.parseBoolean(e.getAttribute("isAbstract")));
        }

        private Property property(Element e, boolean block) {
            var type = type(e);
            var role = e.getAttribute("aggregation").equals("composite") ? Property.Role.PART
                    : block && Model.PRIMITIVES.contains(type) ? Property.Role.VALUE : Property.Role.ATTRIBUTE;
            return new Property(requiredId(e), e.getAttribute("name"), documentation(e), type, multiplicity(e), role);
        }

        private Operation operation(Element e) {
            var params = new ArrayList<Property>();
            String result = "void";
            Multiplicity bounds = Multiplicity.ONE;
            boolean hasReturn = false;
            for (var p : children(e, "ownedParameter")) {
                switch (p.getAttribute("direction")) {
                    case "return" -> {
                        if (hasReturn) fail(p, "Multiple return parameters are not supported");
                        hasReturn = true;
                        result = type(p);
                        bounds = multiplicity(p);
                    }
                    case "", "in" -> params.add(property(p, false));
                    default -> fail(p, "Unsupported parameter direction " + p.getAttribute("direction"));
                }
            }
            return new Operation(requiredId(e), e.getAttribute("name"), documentation(e), result, bounds, params);
        }

        private Association readAssociation(Element e) {
            var ends = new LinkedHashMap<String, Element>();
            for (var end : children(e, "ownedEnd")) ends.put(requiredId(end), end);
            if (e.hasAttribute("memberEnd")) {
                for (var id : e.getAttribute("memberEnd").trim().split("\\s+")) ends.put(id, reference(e, id));
            }
            for (var member : children(e, "memberEnd")) {
                String id = xmi(member, "idref");
                ends.put(id, reference(e, id));
            }
            if (ends.size() != 2) fail(e, "Only binary associations with two member ends are supported");
            var list = new ArrayList<>(ends.values());
            var a = list.get(0);
            var b = list.get(1);
            return new Association(requiredId(e), e.getAttribute("name"), documentation(e), type(a), type(b),
                    a.getAttribute("name"), b.getAttribute("name"), multiplicity(a), multiplicity(b));
        }

        private String type(Element e) {
            if (e.hasAttribute("type")) return primitiveIds.getOrDefault(e.getAttribute("type"), e.getAttribute("type"));
            var types = children(e, "type");
            if (types.isEmpty()) return "";
            var node = types.get(0);
            if (node.hasAttribute("href")) {
                String href = node.getAttribute("href");
                String fragment = href.substring(href.lastIndexOf('#') + 1);
                if ((href.startsWith("pathmap://UML_LIBRARIES/") || href.startsWith("http://www.omg.org/spec/UML/"))
                        && Model.PRIMITIVES.contains(fragment)) return fragment;
                fail(e, "Unsupported external type reference " + href);
            }
            String id = xmi(node, "idref");
            return primitiveIds.getOrDefault(id, id);
        }

        private Multiplicity multiplicity(Element e) {
            return new Multiplicity(bound(e, "lower", 1), bound(e, "upper", 1));
        }

        private int bound(Element e, String name, int fallback) {
            String value = e.getAttribute(name);
            var children = children(e, name + "Value");
            if (!children.isEmpty()) value = children.get(0).hasAttribute("value") ? children.get(0).getAttribute("value") : "0";
            if (value.isEmpty()) return fallback;
            if (value.equals("*") && name.equals("upper")) return -1;
            try { return Integer.parseInt(value); }
            catch (NumberFormatException ex) { fail(e, "Invalid " + name + " multiplicity: " + value); return fallback; }
        }

        private void checkReferences(Model model) {
            for (var c : model.classifiers()) {
                c.properties().forEach(p -> checkType(model, p.id(), p.type()));
                for (var op : c.operations()) {
                    if (!op.returnType().equals("void")) checkType(model, op.id(), op.returnType());
                    op.parameters().forEach(p -> checkType(model, p.id(), p.type()));
                }
                for (var ref : c.generalizations()) checkClassifier(model, c.id(), ref);
                for (var ref : c.interfaces()) checkClassifier(model, c.id(), ref);
            }
            for (var a : model.associations()) {
                checkClassifier(model, a.id(), a.source());
                checkClassifier(model, a.id(), a.target());
            }
        }

        private void checkType(Model model, String id, String type) {
            if (!type.isEmpty() && !Model.PRIMITIVES.contains(type)) checkClassifier(model, id, type);
        }

        private void checkClassifier(Model model, String id, String ref) {
            if (model.classifier(ref).isEmpty()) throw new ModelParseException(file, id, "Missing classifier reference " + ref);
        }

        private Element reference(Element e, String id) {
            var result = ids.get(id);
            if (result == null) fail(e, "Missing reference " + id);
            return result;
        }

        private String requiredId(Element e) {
            String id = xmi(e, "id");
            if (id.isBlank()) fail(e, "Missing XMI id on " + local(e));
            return id;
        }

        private void fail(Element e, String message) {
            String id = xmi(e, "id");
            throw new ModelParseException(file, id.isEmpty() ? "<missing>" : id, message);
        }
    }

    private static String xmi(Element e, String name) {
        var attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            var a = attrs.item(i);
            if (name.equals(a.getLocalName()) && a.getNamespaceURI() != null && a.getNamespaceURI().contains("XMI")) return a.getNodeValue();
        }
        return "";
    }

    private static String kind(Element e) {
        String type = xmi(e, "type");
        return type.substring(type.indexOf(':') + 1);
    }

    private static String local(Element e) { return e.getLocalName() == null ? e.getTagName() : e.getLocalName(); }

    private static List<Element> children(Element e, String name) {
        var result = new ArrayList<Element>();
        for (Node node = e.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element child && (name == null || local(child).equals(name))) result.add(child);
        }
        return result;
    }

    private static String documentation(Element e) {
        var result = new ArrayList<String>();
        for (var comment : children(e, "ownedComment")) {
            if (comment.hasAttribute("body")) result.add(comment.getAttribute("body"));
            for (var body : children(comment, "body")) result.add(body.getTextContent());
        }
        return String.join("\n", result);
    }
}
