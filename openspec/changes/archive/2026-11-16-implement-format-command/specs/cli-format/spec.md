## Purpose

Defines the behavior of the `yao format` CLI command for formatting Oberon source files, including input/output modes, options, and failure handling.

## ADDED Requirements

### Requirement: Show format command help
The `format` subcommand SHALL display usage/help information when invoked without formatting arguments.

#### Scenario: No arguments
- **WHEN** the user runs `yao format` with no file, filter, or mode arguments
- **THEN** the command prints help/usage for the `format` subcommand
- **THEN** the command exits with code `0`

### Requirement: Format a single file to stdout
The `format` subcommand SHALL format a specified Oberon source file and write the formatted source to standard output when no output-destination option is provided.

#### Scenario: Single file to terminal
- **WHEN** the user runs `yao format <file>` where `<file>` is an existing readable Oberon source file and neither `--output-file` nor `--same` nor `--all` is specified
- **THEN** the formatted source is written to standard output
- **THEN** the input file is not modified

### Requirement: Format a single file to an output file
The `format` subcommand SHALL write formatted source to a user-specified output file on success.

#### Scenario: Output file on success
- **WHEN** the user runs `yao format <file> -o <new-file>` or `yao format <file> --output-file <new-file>` and formatting succeeds
- **THEN** `<new-file>` contains the formatted source
- **THEN** the input `<file>` is not modified

#### Scenario: Output file not overwritten on failure
- **WHEN** the user runs `yao format <file> -o <new-file>` and formatting fails
- **THEN** `<new-file>` is not created or overwritten by formatted output
- **THEN** the command reports the failure and exits with a non-zero code

### Requirement: Format a single file in place
The `format` subcommand SHALL support replacing a source file's content with formatted source on success.

#### Scenario: Same file replacement on success
- **WHEN** the user runs `yao format <file> -s` or `yao format <file> --same` and formatting succeeds
- **THEN** `<file>` contains the formatted source

#### Scenario: Same file preserved on failure
- **WHEN** the user runs `yao format <file> -s` or `yao format <file> --same` and formatting fails
- **THEN** the original content of `<file>` is preserved unchanged
- **THEN** the command reports the failure and exits with a non-zero code

### Requirement: Format multiple files by filter
The `format` subcommand SHALL support package-style formatting of files matching a filter in the current directory, with optional recursion.

#### Scenario: All matching files in current directory
- **WHEN** the user runs `yao format <filter> --all` or `yao format <filter> -a`
- **THEN** the command selects files matching `<filter>` in the current directory
- **THEN** each successfully formatted matched file is replaced in place with its formatted content

#### Scenario: Recursive matching
- **WHEN** the user runs `yao format <filter> --all --recurse` or equivalent `-a -r`
- **THEN** the command selects files matching `<filter>` in the current directory and its subdirectories
- **THEN** each successfully formatted matched file is replaced in place with its formatted content

#### Scenario: Recurse ignored without all
- **WHEN** the user provides `--recurse`/`-r` without `--all`/`-a`
- **THEN** the recurse flag has no effect on file selection beyond the applicable non-`--all` mode

#### Scenario: All takes priority over output file
- **WHEN** the user specifies `--all` together with `--output-file`/`-o`
- **THEN** package processing (`--all`) behavior takes priority over single `--output-file` behavior

### Requirement: Configure indentation
The `format` subcommand SHALL allow users to configure indentation style and length.

#### Scenario: Indent type and length options
- **WHEN** the user provides `-i`/`--indent` with value `SPACES` or `TABS` (case-insensitive) and/or `-l`/`--length` with a valid length
- **THEN** formatting uses the specified indentation style and length
- **THEN** spaces length MUST be within `[1, 8]`
- **THEN** tabs length MUST be within `[1, 2]`, meaning that number of tab characters is emitted per indent level

#### Scenario: Invalid indentation options
- **WHEN** the user provides an unsupported indent type or an out-of-range length
- **THEN** the command reports an invalid-argument error and does not modify or create formatted output files

### Requirement: Preserve files on formatting failure
The formatter SHALL not replace original or target content when formatting cannot be completed successfully.

#### Scenario: Parse or format error
- **WHEN** the formatter cannot produce a valid formatted result for a source input (for example due to lex/parse/format errors)
- **THEN** the command does not overwrite the original file and does not create or overwrite a requested output file with formatted content
- **THEN** the command reports the failure and returns a non-zero exit code

### Requirement: Format Oberon modules using existing formatter pipeline
The `format` subcommand SHALL format Oberon source using the project's existing CST-based formatter pipeline.

#### Scenario: Known module formatting
- **WHEN** the command formats `Calc.Mod` from `yaoberon-cli` test resources using default formatting settings
- **THEN** the produced output matches `CalcExpected.Mod` from `yaoberon-cli` test resources

#### Scenario: Automated CLI test
- **WHEN** JUnit tests execute `FormatCommand` via `new CommandLine(new FormatCommand()).execute(args)` against `src/test/resources/Calc.Mod`
- **THEN** the test verifies the command formats the input to the expected content of `src/test/resources/CalcExpected.Mod`
