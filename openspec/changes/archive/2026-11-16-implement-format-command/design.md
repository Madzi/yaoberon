## Context

See `proposal.md` (Why/What Changes) for motivation and scope. `FormatCommand` in `yaoberon-cli` is currently a stub, while `Formatter` (`yaoberon-cli`) already formats from a `SourceFile` and `GreenNode` root. The codebase includes `yaoberon-source`, `yaoberon-lexer`, and `yaoberon-parser` modules and uses Picocli for CLI commands and JUnit 5 for tests.

Constraints: Java 25, Maven multimodule build, JPMS modules, no new external dependencies expected. Planning artifacts define behavior; implementation follows in a later apply phase.

## Goals / Non-Goals

**Goals:**
- Wire `FormatCommand` to parse Oberon source into a CST/`GreenNode` and format it via the existing `Formatter`.
- Support the CLI modes/options specified in `specs/cli-format/spec.md` (help, stdout, `-o/--output-file`, `-s/--same`, `--all/--recurse` with `<filter>`, `-i/--indent`, `-l/--length`).
- Enforce safe-write behavior: never replace original or target content when formatting fails.
- Provide JUnit coverage using `Calc.Mod`/`CalcExpected.Mod` resources and Picocli command execution.

**Non-Goals:**
- Rewriting `Formatter` formatting rules beyond what is needed to satisfy the specified behavior/tests.
- Supporting additional languages, editor integrations, or format-on-save/LSP formatting workflows.
- Introducing new external libraries or changing unrelated CLI commands.

## Decisions

### 1. Use existing lexer/parser/CST pipeline with `Formatter`
- **Decision:** `FormatCommand` will read source text, construct a `SourceFile`, lex/parse it through existing project APIs, obtain the `GreenNode` root, and pass it to `Formatter`.
- **Rationale:** Reuses existing project components and aligns formatting with parsed structure/trivia handling already modeled by `Formatter`.
- **Alternatives:** Token-only or text-only formatting (simpler to wire but diverges from existing CST-based formatter and risks inconsistent results).

### 2. Picocli options/parameters model CLI modes explicitly
- **Decision:** Implement `FormatCommand` as a Picocli command with:
  - optional positional input (`file` or `filter`, depending on mode/help context),
  - `-o/--output-file` (single output path),
  - `-s/--same` (in-place single file),
  - `-a/--all` (package mode, filter-driven from current directory),
  - `-r/--recurse` (effective only with `--all`),
  - `-i/--indent` (case-insensitive enum/string mapped to spaces/tabs),
  - `-l/--length` (validated integer).
- **Rationale:** Matches requested UX and allows clear precedence/validation rules in one place.
- **Alternatives:** Separate subcommands for modes (clearer separation but changes requested command shape).

### 3. Safe formatting transaction
- **Decision:** For `-o` and in-place modes (`-s` and `--all`), compute formatted content fully in memory first; only write/replace files after successful formatting. On any read/lex/parse/format/write-precondition failure, leave originals untouched and do not emit partial formatted targets.
- **Rationale:** Directly satisfies "keep original file..." failure behavior and avoids data loss/corruption.
- **Alternatives:** Stream/overwrite eagerly (simpler but unsafe on mid-process failure).

### 4. Failure and exit-code policy
- **Decision:** Return `0` on successful execution (including help with no args). Return non-zero for invalid arguments, no matches when matches are required by mode, or per-file formatting failures. In `--all` mode, continue attempting other files but report failures and exit non-zero if any file failed (assumption: package command should surface partial failure while preserving failed originals).
- **Rationale:** Makes automation/tests reliable and preserves as much successful work as possible in batch mode without hiding errors.
- **Alternatives:** Fail-fast on first error in batch mode (simpler, but less useful for package formatting).

### 5. Indent configuration mapping
- **Decision:** Build `Formatter` indentation string from options:
  - `SPACES`: `" ".repeat(length)` with length in `[1,8]`.
  - `TABS`: `"\t".repeat(length)` with length in `[1,2]` (N tab characters per indent level).
  - Defaults (assumption): `SPACES`, length `2` when omitted.
- **Rationale:** Matches clarified semantics, including tab length meaning.
- **Alternatives:** Fixed tab width display semantics or single-tab-only behavior (conflicts with requested bounds).

### 6. Filter-based `--all` selection
- **Decision:** Interpret `<filter>` in `--all` mode as a filename/path glob pattern relative to the current working directory; without `--recurse` match current directory entries, with `--recurse` walk subdirectories and match files by the same pattern semantics. Users may pass explicit filenames/extensions; `.Mod` is a default convention, not a hard-coded restriction for explicit inputs.
- **Rationale:** Aligns with clarified `yao format <filter> --all [--recurse]` form and removes `<dir>`-argument ambiguity.
- **Alternatives:** Treat argument as directory path (removed by clarification) or rely solely on shell glob expansion (platform/shell dependent).

### 7. Test strategy
- **Decision:** Add JUnit tests in `yaoberon-cli` that invoke `new CommandLine(new FormatCommand()).execute(args)`:
  - primary fixture test formats `src/test/resources/Calc.Mod` and asserts result equals `src/test/resources/CalcExpected.Mod` (stdout or equivalent command output path as specified by the test setup),
  - additional tests for option validation/precedence and safe-failure behavior as needed by spec scenarios.
- **Rationale:** Verifies CLI contract and satisfies requested acceptance coverage.
- **Alternatives:** Unit-test only `Formatter` directly (insufficient for CLI behavior).

## Risks / Trade-offs

- [Parser/formatter limitations for some inputs] → Preserve originals on failure, return non-zero, and base acceptance on provided fixtures; iterate formatter/parser separately if gaps are found.
- [Glob/filter semantics differ from shell/user expectations] → Document and test intended pattern behavior; keep matching relative to current directory and explicit in spec/design.
- [Batch mode partial success can confuse users] → Emit per-file success/failure reporting and non-zero aggregate exit when any file fails.
- [Default indent assumption may differ from project preference] → Recorded as assumption in proposal; easy to change in implementation/tests if corrected.

## Migration Plan

- No data migration. Add/modify CLI behavior and tests in `yaoberon-cli`.
- Rollback: revert `FormatCommand`/related CLI and test changes.

## Open Questions

- None blocking; remaining defaults/semantics are captured as assumptions in `proposal.md` and can be refined during apply/review if needed.
