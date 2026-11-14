# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepchangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Maven module `yaoberon-cli` with picocli CLI entry point
- Initial CLI command name `yao`
- `module-info.java` for module `io.github.madzi.yaoberon.cli` with picocli requirement and package export
- Packaging with moditect `create-runtime-image` and jpackage-maven-plugin
- `make prepare-release` target for runnable application build
- Subcommand `yao lsp` via `LspCommand` with picocli `@Command` annotation
- Module `yaoberon-i18n` with JPMS module `io.github.madzi.yaoberon.i18n` and `MessageService` interface
- `MessageService.Key` nested interface and `BaseMessageService` implementation using `ResourceBundle`, API `text(Key)` / `text(Key, Object...)`
- `yaoberon-cli` depends on `yaoberon-i18n`, uses `Messages` helper, `MessageKeys` enum, `i18n/messages.properties`, jline terminal output in `LspCommand` and `FormatCommand`

### Changed
- Removed module-specific `<build>` section from `yaoberon-cli/pom.xml`, relying on parent pluginManagement
- Updated `@Command` name from `yaoberon-cli` to `yao`
- jpackage application name set to `yao`
- Updated moditect-maven-plugin version from 1.6.0 to 1.3.0.Final (latest available)
- Switched jpackage-maven-plugin from org.paulhoughton:1.6.0 to org.panteleyev:1.8.0
- Removed unsupported `<compress>` parameter from moditect create-runtime-image
- `module-info.java` opens `io.github.madzi.yaoberon.cli` and `io.github.madzi.yaoberon.cli.command` to `info.picocli` for subcommand instantiation via reflection

