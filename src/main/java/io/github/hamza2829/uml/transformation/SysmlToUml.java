package io.github.hamza2829.uml.transformation;

import io.github.hamza2829.uml.model.*;
import io.github.hamza2829.uml.model.Package;
import java.util.*;

/** Pure transformation: no changes to the source model, and stable ids in the result. */
public final class SysmlToUml {
    public Model transform(Model source) {
        var associations = new ArrayList<>(source.associations());
        var classifiers = new ArrayList<ClassifierModel>();
        var counts = new HashMap<String, Integer>();
        source.classifiers().forEach(c -> counts.merge(c.name(), 1, Integer::sum));
        var names = qualifiedNames(source);
        var usedNames = new HashSet<String>();
        for (var c : source.classifiers()) {
            var properties = new ArrayList<Property>();
            for (var p : c.properties()) {
                if (p.role() == Property.Role.PART) {
                    associations.add(new Association(p.id() + "_association", c.name() + "_" + p.name(), p.documentation(),
                            c.id(), p.type(), "", p.name(), Multiplicity.ONE, p.multiplicity()));
                } else properties.add(new Property(p.id(), p.name(), p.documentation(), p.type(), p.multiplicity(), Property.Role.ATTRIBUTE));
            }
            String name = counts.get(c.name()) > 1 ? names.get(c.id()).replace('.', '_') : c.name();
            // Prefix an uppercase letter because source package names start with lowercase letters.
            if (counts.get(c.name()) > 1) name = "Flat_" + name;
            String base = name;
            int suffix = 2;
            while (!usedNames.add(name)) name = base + "_" + suffix++;
            classifiers.add(new ClassifierModel(c.id(), name, c.documentation(), c.kind(), properties,
                    c.operations(), c.literals(), c.generalizations(), c.interfaces(),
                    c.stereotypes().stream().filter(s -> !s.name().equals("Block")).toList(), c.isAbstract()));
        }
        return new Model(source.source(), List.of(new Package("flat", "flat", "Flattened UML model", List.of(), classifiers, associations)));
    }

    public static Map<String, String> qualifiedNames(Model model) {
        var result = new LinkedHashMap<String, String>();
        for (var p : model.packages()) names(p, "", result);
        return Collections.unmodifiableMap(result);
    }

    private static void names(Package p, String prefix, Map<String, String> result) {
        String path = prefix.isEmpty() ? p.name() : prefix + "." + p.name();
        p.classifiers().forEach(c -> result.put(c.id(), path + "." + c.name()));
        p.packages().forEach(child -> names(child, path, result));
    }
}
