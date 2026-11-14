# AGENTS.md

- Intended scope: several tools around Oberon 07 compiler.
- Tech stack: Java, Maven multimodule. Root pom.xml present.
- Root coordinates: groupId io.github.madzi, artifactId yaoberon, version 1.0-SNAPSHOT, packaging pom.
- Java target: 25 via maven.compiler.release.
- Maven Wrapper: 3.9.16, ./mvnw available. Wrapper version 3.3.4.
- Managed BOMs/deps:
  - slf4j-bom 2.1.0-alpha1
  - junit-bom 5.14.0
  - org.eclipse.lsp4j 0.24.0
  - info.picocli 4.7.7 with picocli-codegen annotation processor
- Build helper: makefile wraps ./mvnw: compile, test, package -DskipTests, build = clean verify, run = exec:java, clean.
- Modules:
  - yaoberon-cli: CLI tools, picocli based. Entry: io.github.madzi.yaoberon.cli.Main, command name `yao`, JPMS module `io.github.madzi.yaoberon.cli`
  - yaoberon-i18n: Internationalization support, JPMS module `io.github.madzi.yaoberon.i18n`, `MessageService` interface
  - Build config centralized in parent pom; modules do not declare own <build> unless needed
- No README, lint config, CI, or instruction files verified yet.
- When modules are added, re-inspect root pom.xml, module poms, and CI before adding concrete commands.
