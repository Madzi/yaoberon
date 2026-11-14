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

### Changed
- Removed module-specific `<build>` section from `yaoberon-cli/pom.xml`, relying on parent pluginManagement
- Updated `@Command` name from `yaoberon-cli` to `yao`
- jpackage application name set to `yao`
- Updated moditect-maven-plugin version from 1.6.0 to 1.3.0.Final (latest available)
- Switched jpackage-maven-plugin from org.paulhoughton:1.6.0 to org.panteleyev:1.8.0
- Removed unsupported `<compress>` parameter from moditect create-runtime-image

### Changed
- Removed module-specific `<build>` section from `yaoberon-cli/pom.xml`, relying on parent pluginManagement
- Updated `@Command` name from `yaoberon-cli` to `yao`

