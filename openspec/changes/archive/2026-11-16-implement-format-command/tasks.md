## 1. CLI contract and options

- [x] 1.1 Implement `FormatCommand` Picocli arguments/options for help/no-arg behavior, single input, `-o/--output-file`, `-s/--same`, `-a/--all` filter mode, `-r/--recurse`, `-i/--indent` (case-insensitive `SPACES`/`TABS`), and `-l/--length`.
- [x] 1.2 Add option validation and precedence rules: spaces length `[1,8]`, tabs length `[1,2]`, `--recurse` ignored without `--all`, and `--all` takes priority over `--output-file`; report invalid arguments without creating or modifying formatted outputs.

## 2. Formatting pipeline integration

- [x] 2.1 Integrate source loading with `yaoberon-source` and lex/parse via existing `yaoberon-lexer`/`yaoberon-parser` APIs to obtain a `GreenNode` root for formatting.
- [x] 2.2 Invoke `io.github.madzi.yaoberon.cli.command.format.Formatter` with indentation derived from options (default assumption: `SPACES`, length `2`; `TABS` length means N tab characters per indent level) and return formatted text on success.
- [x] 2.3 Implement failure handling so parse/format/read errors are reported, exit non-zero, and original files/requested output targets are not overwritten or created with formatted content.

## 3. Output and batch modes

- [x] 3.1 Implement single-file stdout mode (`yao format <file>` without `-o/-s/--all`) and help/usage output with exit code `0` for `yao format` with no formatting arguments.
- [x] 3.2 Implement single-file `--output-file` and `--same` modes with safe-write behavior (write/replace only after successful formatting).
- [x] 3.3 Implement `--all <filter>` package processing in the current directory and `--recurse` subdirectory traversal, formatting each matched file in place on success, preserving failed files, and returning a non-zero exit code if any file fails.

## 4. Tests and verification

- [x] 4.1 Add a JUnit test in `yaoberon-cli` that runs `new CommandLine(new FormatCommand()).execute(args)` against `src/test/resources/Calc.Mod` and asserts the formatted result matches `src/test/resources/CalcExpected.Mod`.
- [x] 4.2 Add/adjust JUnit tests for CLI behavior as needed (help/no-args, invalid indent options, precedence/ignored flags, and failure-preservation cases) consistent with `specs/cli-format/spec.md`.
- [x] 4.3 Run relevant module tests (at minimum `yaoberon-cli`) and confirm the new/updated formatting command tests pass.
