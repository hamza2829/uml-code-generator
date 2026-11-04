package io.github.hamza2829.uml.generation;

import java.util.Map;
import java.util.regex.*;

/** Single pass substitution, so inserted model documentation is never interpreted as a template. */
public final class TemplateEngine {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Z_]+)}}");

    public String render(String template, Map<String, String> values) {
        var matcher = TOKEN.matcher(template);
        var out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) throw new GenerationException("Missing template value " + matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
