# Maven Maintenance

Maintain Maven projects with small, hierarchy-aware changes. Preserve the project's existing Java, Maven, test, formatting, repository, and wrapper conventions.

## Inspect before editing

1. Locate every `pom.xml`, Maven wrapper, Maven configuration, CI build definition, and relevant Java test.
2. Determine the reactor structure: identify the root aggregator, parent POMs, modules, packaging types, imported BOMs, and any inherited dependency or plugin management.
3. Search for the target artifact across all POMs before changing it. Do not duplicate management already supplied by an inherited parent or imported BOM.
4. Check the working tree and preserve unrelated changes.

Use Maven Wrapper (`./mvnw`) when it is present; otherwise use the repository's established Maven command.

## Maven Wrapper and reactor root

Treat the directory containing `.mvn/` and the root aggregator `pom.xml` as the multi-module project's Maven root. When a Maven project has no wrapper, create one from that root with:

```bash
mvn wrapper:wrapper
```

Commit the generated `.mvn/` directory and wrapper launchers when repository policy permits. Do not regenerate or overwrite an existing wrapper without a specific maintenance reason. Use the generated `./mvnw` for subsequent commands.

## Dependency-version policy

Put every dependency version the project controls in the root aggregator POM's `<dependencyManagement>` section, and omit `<version>` from dependency declarations that consume it. In a multi-module project, do not add `<dependencyManagement>` to child POMs.

Define every controlled dependency and plugin version once as a root-POM property, using the established naming convention; use names such as `<version.slf4j>2.0.1</version.slf4j>` when the project has no convention. Reference the property from `<dependencyManagement>` or `<pluginManagement>`, never from an ordinary dependency declaration.

Use this pattern when a project controls an artifact version:

```xml
<properties>
  <version.example-client>1.2.3</version.example-client>
</properties>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>example-client</artifactId>
      <version>${version.example-client}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.example</groupId>
    <artifactId>example-client</artifactId>
  </dependency>
</dependencies>
```

For a multi-module reactor, keep all dependency management in the root aggregator POM and remove redundant child-module versions and child `<dependencyManagement>` sections. If a module requires a distinct version, add its root-level management entry and document the intentional scope through the POM structure or an existing comment convention.

Use an imported BOM through the root `<dependencyManagement>` when the ecosystem provides one. Put the BOM version in a root property, keep `type` `pom` and `scope` `import`, and leave versions off dependencies managed by that BOM. Do not create duplicate explicit management entries merely to restate the BOM.

Keep `<version>` in a dependency declaration only when Maven requires it or it deliberately overrides the inherited/BOM-managed version. Before accepting an override, confirm that the scope is intentional and explain the compatibility or security reason.

Do not apply this dependency rule to parent POM versions. Manage the version of every used build plugin in the root POM's `<pluginManagement>`, including standard plugins such as `maven-clean-plugin` and `maven-compiler-plugin`; declare the plugin without a version where it is used. Keep each managed plugin version in a root property.

## POM layout and profiles

Do not add empty lines to `pom.xml` files. Preserve the existing element order and indentation, and keep related XML elements adjacent.

Use profiles to bound logical dependency and plugin groups. Give each profile a purpose-specific identifier, such as `logging` for logging dependencies or `testing` for test dependencies and test-related plugins. Keep unrelated dependencies and plugins outside that profile.

Before moving existing configuration into a profile, preserve the build's current activation behavior. Do not make a profile opt-in or active by default if that would silently change dependency resolution, tests, packaging, or CI behavior. Use explicit activation only when the repository's build contract requires it.

## Make safe changes

- Preserve dependency scopes, optional flags, exclusions, classifiers, types, and comments unless the requested maintenance change requires them.
- Respect Maven's nearest-definition and dependency-mediation behavior; inspect the effective POM or dependency tree before resolving a conflict by changing a version.
- Avoid version ranges, `LATEST`, `RELEASE`, snapshots, and unrelated upgrades unless explicitly requested.
- Do not modify repository, distribution-management, credentials, signing, deployment, or publishing configuration without explicit user authorization.
- Keep plugin, Java, and framework upgrades separate from a dependency-management refactor unless required for a successful build.

## Verify proportionally

Run the narrowest relevant command first, for example:

```bash
./mvnw -pl :affected-module -am test
```

For POM-only dependency changes, also use an appropriate inspection command, such as:

```bash
./mvnw -pl :affected-module dependency:tree -Dincludes=groupId:artifactId
```

Use `validate`, `test`, or `verify` according to the repository's existing practice. Do not add `-U`, skip tests, run release/deploy goals, or write Maven settings containing credentials unless the user explicitly asks. Report the exact verification run and anything not verified.
