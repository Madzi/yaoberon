package io.github.madzi.yaoberon.cli.command;

import io.github.madzi.yaoberon.cli.Main;
import io.github.madzi.yaoberon.cli.command.format.FormatConfig;
import io.github.madzi.yaoberon.format.FormatFailureException;
import io.github.madzi.yaoberon.format.Formatter;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;
import io.github.madzi.yaoberon.format.FormatResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "format", description = "Format Oberon source", mixinStandardHelpOptions = false)
public class FormatCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "<file|filter>", description = "Source file or filter for --all mode")
    String input;

    @Option(names = {"-o", "--output-file"}, paramLabel = "<file>", description = "Write formatted output to file")
    Path outputFile;

    @Option(names = {"-s", "--same"}, description = "Format file in place")
    boolean same;

    @Option(names = {"-a", "--all"}, description = "Format all files matching filter in current directory")
    boolean all;

    @Option(names = {"-r", "--recurse"}, description = "Recurse into subdirectories (only with --all)")
    boolean recurse;

    @Option(names = {"-i", "--indent"}, paramLabel = "<SPACES|TABS>", description = "Indent type")
    String indent;

    @Option(names = {"-l", "--length"}, paramLabel = "<n>", description = "Indent length")
    Integer length;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    boolean helpRequested;

    @Spec
    CommandSpec spec;

    PrintWriter outWriter;
    PrintWriter errWriter;

    void setWriters(PrintWriter out, PrintWriter err) {
        this.outWriter = out;
        this.errWriter = err;
    }

    @Override
    public Integer call() {
        try (Terminal terminal = (outWriter == null || errWriter == null) ? TerminalBuilder.builder().system(true).build() : null) {
            PrintWriter out = outWriter != null ? outWriter : terminal.writer();
            PrintWriter err = errWriter != null ? errWriter : terminal.writer();

            if (!all && recurse) {
                recurse = false;
            }

            boolean noModeArgs = input == null && outputFile == null && !same && !all && indent == null && length == null;
            if (noModeArgs) {
                spec.commandLine().usage(out);
                out.flush();
                err.flush();
                return 0;
            }

            FormatConfig indentConfig;
            try {
                indentConfig = resolveIndentConfig(indent, length);
            } catch (IllegalArgumentException e) {
                err.println(e.getMessage());
                err.flush();
                return 1;
            }

            try {
                if (all) {
                    int code = runAll(indentConfig, out, err);
                    out.flush();
                    err.flush();
                    return code;
                }
                if (input == null) {
                    err.println(Main.I18N.text(MessageKeys.FORMAT_MISSING_INPUT));
                    err.flush();
                    return 1;
                }
                Path inputPath = Path.of(input);
                String formatted = formatFile(inputPath, indentConfig);
                if (same) {
                    writeFileSafely(inputPath, formatted);
                    out.flush();
                    return 0;
                }
                if (outputFile != null) {
                    writeFileSafely(outputFile, formatted);
                    out.flush();
                    return 0;
                }
                out.print(formatted);
                out.flush();
                return 0;
            } catch (final FormatFailureException exception) {
                err.println(exception.getMessage());
                err.flush();
                return 1;
            } catch (final IOException exception) {
                String msg = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                err.println(msg);
                err.flush();
                return 1;
            }
        } catch (IOException e) {
            return 1;
        }
    }

    private int runAll(FormatConfig indentConfig, PrintWriter out, PrintWriter err) {
        if (input == null || input.isBlank()) {
            err.println(Main.I18N.text(MessageKeys.FORMAT_MISSING_FILTER));
            return 1;
        }
        List<Path> files;
        try {
            Path baseDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            files = collectMatchingFiles(baseDir, input, recurse);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            err.println(Main.I18N.text(MessageKeys.FORMAT_SCAN_FAILED, msg));
            return 1;
        }
        if (files.isEmpty()) {
            err.println(Main.I18N.text(MessageKeys.FORMAT_NO_MATCHES, input));
            return 1;
        }
        boolean anyFailed = false;
        for (Path file : files) {
            try {
                String formatted = formatFile(file, indentConfig);
                writeFileSafely(file, formatted);
                out.println(Main.I18N.text(MessageKeys.FORMAT_FORMATTED_FILE, file));
            } catch (final IOException exception) {
                anyFailed = true;
                String msg = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                err.println(Main.I18N.text(MessageKeys.FORMAT_FILE_FAILED, file, msg));
            }
        }
        return anyFailed ? 1 : 0;
    }

    private List<Path> collectMatchingFiles(Path baseDir, String filter, boolean recursive) throws IOException {
        final PathMatcher matcher = buildMatcher(filter);
        final List<Path> matches = new ArrayList<>();
        if (!recursive) {
            try (var stream = Files.list(baseDir)) {
                stream.filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p.getFileName()))
                    .sorted()
                    .forEach(matches::add);
            }
            return matches;
        }
        Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    Path rel = baseDir.relativize(file);
                    if (matcher.matches(rel) || matcher.matches(file.getFileName())) {
                        matches.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        matches.sort(Path::compareTo);
        return matches;
    }

    private PathMatcher buildMatcher(String filter) {
        String normalized = filter.replace('\\', '/');
        if (!normalized.contains("*") && !normalized.contains("?") && !normalized.contains("[") && !normalized.contains("{")) {
            normalized = "**/" + normalized;
        }
        try {
            return FileSystems.getDefault().getPathMatcher("glob:" + normalized);
        } catch (Exception e) {
            // Fallback: treat as literal file name
            String literal = normalized;
            return path -> path != null && path.getFileName() != null && path.getFileName().toString().equals(literal);
        }
    }

    private String formatFile(Path inputPath, FormatConfig indentConfig) throws IOException {
        if (!Files.exists(inputPath)) {
            throw new FormatFailureException(Main.I18N.text(MessageKeys.FORMAT_INPUT_NOT_FOUND, inputPath));
        }
        if (!Files.isRegularFile(inputPath)) {
            throw new FormatFailureException(Main.I18N.text(MessageKeys.FORMAT_INPUT_NOT_FILE, inputPath));
        }
        String content = Files.readString(inputPath, StandardCharsets.UTF_8);
        try {
            Formatter formatter = Formatter.create(indentConfig.indent());
            FormatResult fres = formatter.format(content);
            return switch (fres) {
                case FormatResult.Successful sres -> sres.formatted();
                case FormatResult.ParseFailed _ -> throw new FormatFailureException(Main.I18N.text(MessageKeys.FORMAT_PARSE_FAILED, inputPath));
                case FormatResult.SyntaxErrors _ -> throw new FormatFailureException(Main.I18N.text(MessageKeys.FORMAT_SYNTAX_ERRORS, inputPath));
            };
        } catch (FormatFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new FormatFailureException(Main.I18N.text(MessageKeys.FORMAT_FAILED, inputPath, msg), e);
        }
    }

    private void writeFileSafely(Path target, String formatted) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(target, formatted, StandardCharsets.UTF_8);
    }

    private FormatConfig resolveIndentConfig(String indentValue, Integer lengthValue) {
        var builder = FormatConfig.builder();
        if (null != indentValue) {
            builder.type(indentValue);
        }
        if (null != lengthValue) {
            builder.count(lengthValue);
        }
        return builder.build();
    }
}
