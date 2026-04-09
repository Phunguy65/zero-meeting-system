# ADDED Requirements

## Requirement: The Android app SHALL generate Java client code from the unified spec

The `openapi-generator-gradle-plugin` SHALL be configured in the Android app's
`build.gradle.kts` to generate Java API interfaces and model classes using the
`java` generator with `retrofit2` library and `gson` serialization from
`openapi/unified-openapi.yaml`.

### Scenario: Gradle plugin generates client code

- **WHEN** the `openApiGenerate` Gradle task runs in the Android app module
- **THEN** Java source files MUST be generated in `build/generated/openapi/`
  under the configured `apiPackage` and `modelPackage`

### Scenario: Generated code compiles

- **WHEN** the Android app compiles after SDK generation
- **THEN** `./gradlew :app:compileDebugJavaWithJavac` MUST succeed with zero
  errors

## Requirement: Generated Android source SHALL be wired into Gradle source sets

The `build/generated/openapi/` directory SHALL be added to the main Java source
set so generated classes are available to application code at compile time.

### Scenario: Application code can import generated classes

- **WHEN** application code in `app/src/main/java/` imports a class from the
  generated `apiPackage` or `modelPackage`
- **THEN** the import MUST resolve and the project MUST compile

## Requirement: Generated Android code SHALL be gitignored

The `build/generated/` directory SHALL be excluded from version control.

### Scenario: Generated files are not tracked

- **WHEN** `git status` is checked after running generation
- **THEN** no files under `frontends/android-app/app/build/generated/` MUST
  appear as untracked or modified
