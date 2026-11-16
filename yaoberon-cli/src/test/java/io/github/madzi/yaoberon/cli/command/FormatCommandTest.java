package io.github.madzi.yaoberon.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class FormatCommandTest {

    @Test
    void formatsCalcModToExpectedOutputOnStdout() throws Exception {
        Path input = Path.of("src/test/resources/Calc.Mod");
        String expected = Files.readString(Path.of("src/test/resources/CalcExpected.Mod"), StandardCharsets.UTF_8);

        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute(input.toString());

        assertEquals(0, exit, swErr.toString());
        assertEquals(expected, swOut.toString());
    }

    @Test
    void printsHelpAndExitsZeroWhenNoArgs() {
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute();

        assertEquals(0, exit);
        assertTrue(swOut.toString().contains("Usage"), swOut.toString());
    }

    @Test
    void invalidIndentLengthReturnsErrorAndDoesNotWriteOutput(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("In.Mod");
        Path output = tempDir.resolve("Out.Mod");
        Files.writeString(input, "MODULE X; END X.", StandardCharsets.UTF_8);

        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute(input.toString(), "-o", output.toString(), "-i", "spaces", "-l", "9");

        assertNotEquals(0, exit);
        assertFalse(Files.exists(output));
        assertTrue(swErr.toString().contains("Invalid indent length"), swErr.toString());
    }

    @Test
    void outputFileIsNotCreatedOnFormattingFailure(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("Bad.Mod");
        Path output = tempDir.resolve("Bad.out.Mod");
        Files.writeString(input, "MODULE Broken; VAR x: INTEGER", StandardCharsets.UTF_8); // intentionally malformed

        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute(input.toString(), "-o", output.toString());

        assertNotEquals(0, exit);
        assertFalse(Files.exists(output), "Output file must not be created on failure");
    }

    @Test
    void sameModePreservesOriginalOnFailure(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("Bad.Mod");
        String original = "MODULE Broken; VAR x: INTEGER";
        Files.writeString(input, original, StandardCharsets.UTF_8);

        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute(input.toString(), "-s");

        assertNotEquals(0, exit);
        assertEquals(original, Files.readString(input, StandardCharsets.UTF_8));
    }

    @Test
    void allModeFormatsMatchingFilesRecursively(@TempDir Path tempDir) throws Exception {
        Path root = tempDir;
        Path a = root.resolve("A.Mod");
        Path sub = Files.createDirectories(root.resolve("sub"));
        Path b = sub.resolve("B.Mod");
        String source = Files.readString(Path.of("src/test/resources/Calc.Mod"), StandardCharsets.UTF_8);
        String expected = Files.readString(Path.of("src/test/resources/CalcExpected.Mod"), StandardCharsets.UTF_8);
        Files.writeString(a, source, StandardCharsets.UTF_8);
        Files.writeString(b, source, StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(out), new PrintWriter(err));
        CommandLine cmd = new CommandLine(command);

        int exit = executeInDir(root, cmd, "*.Mod", "--all", "--recurse");
        assertEquals(0, exit, "stderr: " + err + "\nstdout: " + out);
        assertEquals(expected, Files.readString(a, StandardCharsets.UTF_8));
        assertEquals(expected, Files.readString(b, StandardCharsets.UTF_8));
    }

    @Test
    void recurseWithoutAllIsIgnoredForSingleFileMode(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("A.Mod");
        String source = Files.readString(Path.of("src/test/resources/Calc.Mod"), StandardCharsets.UTF_8);
        String expected = Files.readString(Path.of("src/test/resources/CalcExpected.Mod"), StandardCharsets.UTF_8);
        Files.writeString(input, source, StandardCharsets.UTF_8);
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        FormatCommand command = new FormatCommand();
        command.setWriters(new PrintWriter(swOut), new PrintWriter(swErr));
        CommandLine cmd = new CommandLine(command);

        int exit = cmd.execute(input.toString(), "-r");
        assertEquals(0, exit, swErr.toString());
        assertEquals(expected, swOut.toString());
    }

    private static int executeInDir(Path dir, CommandLine cmd, String... args) {
        String old = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", dir.toString());
            return cmd.execute(args);
        } finally {
            System.setProperty("user.dir", old);
        }
    }
}
