## Why

The CLI already exposes a `format` subcommand and contains formatter logic, but `FormatCommand` is currently a stub and cannot format Oberon source files. Implementing it provides a usable formatting workflow for Oberon 07 sources through the `yao` CLI.

## What Changes

- Implement `FormatCommand` in `yaoberon-cli` to format Oberon source files using the existing lexer/parser/CST pipeline and `Formatter`.
- Add CLI behavior:
  - `yao format` prints format command help/usage and exits with code `0`.
  - `yao format <file.Mod>` formats the file and writes formatted source to stdout.
  - `yao format <file.Mod> -o <new-file.Mod>` / `--output-file <new-file.Mod>` writes formatted output to the specified file on success.
  - `yao format <file.Mod> -s` / `--same` reformats the file in place on success.
  - `yao format <filter> --all` / `-a` formats all files matching `<filter>` in the current directory in place (package processing).
  - `yao format <filter> --all --recurse` / `-r` formats matching files in the current directory and subdirectories in place.
- Add formatting options:
  - `-i` / `--indent` accepts `SPACES` or `TABS` (case-insensitive).
  - `-l` / `--length` sets indent length: spaces `[1, 8]`, tabs `[1, 2]` (for tabs, emit N tab characters per indent level).
- Define option precedence/constraints: `--all` takes priority over `--output-file`; `--recurse` is ignored unless `--all` is used; `--output-file`/in-place writes occur only when formatting succeeds.
- Keep original content unchanged when a source cannot be formatted (do not create/overwrite target output on formatting failure); command reports failure appropriately.
- Add JUnit coverage in `yaoberon-cli` that invokes `FormatCommand` via `new CommandLine(new FormatCommand()).execute(args)` and formats `src/test/resources/Calc.Mod`, asserting output matches `src/test/resources/CalcExpected.Mod`.

## Capabilities

### New Capabilities
- `cli-format`: CLI formatting command for Oberon source files, including argument/option handling, file selection modes, formatter invocation, output behavior, and error handling.

### Modified Capabilities
<!-- None. No existing specs under openspec/specs/. -->

## Impact

- Affected code: `yaoberon-cli/src/main/java/io/github/madzi/yaoberon/cli/command/FormatCommand.java` (primary), likely supporting CLI/i18n or helper code in `yaoberon-cli`, and reuse of existing `io.github.madzi.yaoberon.cli.command.format.Formatter` plus lexer/parser/source APIs.
- Tests/resources: `yaoberon-cli/src/test/java/...` and existing `yaoberon-cli/src/test/resources/Calc.Mod`, `CalcExpected.Mod`.
- Dependencies: no new external dependencies expected; uses existing project modules (`yaoberon-source`, `yaoberon-lexer`, `yaoberon-parser`) and Picocli/JUnit already configured.

## Assumptions

- Default extension guidance is `.Mod`, but users may provide full filenames with any extension when passed explicitly.
- Default indent settings when options are omitted: `SPACES` with length `2` (recorded assumption; can be adjusted during implementation review if project convention differs).
- `yao format <file.Mod>` without `-o`/`-s` writes formatted output to stdout only.
- `-s/--same` is single-file in-place mode and is not combined with `-o` except where overridden by `--all` precedence rules above.
- In `--all` mode, `<filter>` is matched against files in the current directory, and with `--recurse` also subdirectories; each matched file is reformatted in place on success and left unchanged on failure.
- `yao format` with no arguments prints help/usage and exits `0`.
- Formatting uses the existing `GreenNode`-based `Formatter` and parser/lexer pipeline; parse/format failures must not replace original content.
