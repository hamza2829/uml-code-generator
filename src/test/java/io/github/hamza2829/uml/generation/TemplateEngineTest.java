package io.github.hamza2829.uml.generation;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateEngineTest {
    @Test void substitutesRepeatedTokensAndLiteralSpecialCharacters() {
        assertEquals("$1\\path $1\\path", new TemplateEngine().render("{{TEXT}} {{TEXT}}", Map.of("TEXT", "$1\\path")));
    }
    @Test void insertedTokensAreNotExecuted() {
        assertEquals("{{MISSING}}", new TemplateEngine().render("{{DOC}}", Map.of("DOC", "{{MISSING}}")));
    }
    @Test void reportsMissingValues() {
        assertEquals("Missing template value NAME", assertThrows(GenerationException.class,
                () -> new TemplateEngine().render("{{NAME}}", Map.of())).getMessage());
    }
}
