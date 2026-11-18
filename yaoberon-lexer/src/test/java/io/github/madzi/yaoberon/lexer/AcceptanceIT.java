package io.github.madzi.yaoberon.lexer;

import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.source.SourceFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class AcceptanceIT {

    private final Lexer lexer = Lexer.create();

    @Test
    void testPOSources() {
        var path = Paths.get("..", "docs", "oberon-src");
        Assertions.assertTrue(path.toFile().exists());
        Assertions.assertDoesNotThrow(() -> Files.walk(path)
                .filter(this::isSourceFile)
                .forEach(this::process));
    }

    private void process(final Path path) {
        try {
            System.out.println("Processing: " + path.getFileName().toString());
            var content = String.join("\n", Files.readAllLines(path));
            var source = SourceFile.create(path.toUri().toString(), 0, content);
            var result = lexer.tokenizeAll(source);
            result.tokens().forEach(token -> {
                if (TokenKind.ERROR == token.kind()) {
                    Assertions.fail("Source contains error: " + token);
                }
            });
        } catch (final IOException exception) {
            Assertions.fail("Unable to read source file: " + path, exception);
        }
    }

    private boolean isSourceFile(final Path path) {
        return path.endsWith(".Mod");
    }
}
