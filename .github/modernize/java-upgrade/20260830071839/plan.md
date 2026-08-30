# Upgrade Plan: sistema-logistica (20260830071839)

- **Generated**: 2026-08-30 09:18:39
- **HEAD Branch**: main
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 17: C:\Program Files\Java\jdk-11\bin (current project JDK used by baseline step; available)
- JDK 21: C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin (available)
- JDK 25: C:\Users\ivand\AppData\Local\jdks\jdk-25.0.2\bin (required for final validation)

**Build Tools**
- Maven 3.9.16: C:\Apache\Maven\apache-maven-3.9.16\bin (project build tool)
- Maven Wrapper: not present in the repository

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

- Upgrade only to the specified LTS target: Java 25.
- Preserve existing project behavior and security controls.
- Prefer the safest incremental path: baseline on current JDK, then upgrade Java target and framework compatibility, then validate with full tests.

## Options

- Working branch: appmod/java-upgrade-20260830071839
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java 25

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| ---------------------- | ------- | ---------------------- | --------------- |
| Java | 17 | 25 | User requested |
| Spring Boot | 3.2.3 | 3.5.x | Java 25 is best supported on Spring Boot 3.5+; 3.2.x predates long-term Java 25 support |
| Maven | 3.9.16 | 3.9.16 | Compatible with Java 25 |
| Vaadin | 24.3.5 | 24.3.5+ | Current version is compatible with current Java 21+ stack; no direct JDK 25 blocker identified |

## Derived Upgrades

- Java 25 target requires a compiler/runtime upgrade from `java.version` 17 to 25.
- The project uses Spring Boot; for Java 25 readiness, Spring Boot should be aligned to the 3.5.x line before final validation.
- Maven is already at 3.9.16, which is compatible with Java 25.

## Impact Analysis

### Subsection: Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| pom.xml | java.version | 17 | upgrade | 25 | User requested |
| pom.xml | org.springframework.boot:spring-boot-starter-parent | 3.2.3 | upgrade | 3.5.x | Align the framework with Java 25 compatibility |

### Subsection: Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| No Java source changes identified in initial project scan. | Build-level compatibility check | Current project uses standard Spring Boot + Vaadin APIs | Verify compilation and fix any Java 25-specific issues surfaced by the compiler/test run | Runtime compatibility validation |

### Subsection: Configuration Changes

| File | Property/Setting | Current | Required Change | Reason |
|------|------------------|---------|-----------------|--------|
| pom.xml | java.version | 17 | 25 | Required by upgrade goal |

### Subsection: CI/CD Changes

| File | Location | Current | Required Change |
|------|----------|---------|----------------|
| None identified in repository root configuration. | N/A | N/A | No explicit CI version pin found in checked-in files |

### Subsection: Risks & Warnings

- **Spring Boot baseline is older than the Java 25 support window**: Boot 3.2.x is not the recommended baseline for the latest LTS runtime. **Mitigation**: upgrade to the 3.5.x line and validate with the full test suite.
- **Java 25 is newer than the current project baseline**: runtime incompatibilities may appear only during compilation or test execution. **Mitigation**: use the compiler/test loop to catch and fix any API or reflection issues immediately.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Confirm the target JDK and build tool are available before starting the upgrade.
  - **Changes to Make**: Validate Java 25 tooling availability and confirm the project build tool.
  - **Verification**: `java -version` and `mvn -v` through the selected JDK path; expected result: Java 25 and Maven 3.9.16 available.

- Step 2: Setup Baseline
  - **Rationale**: Capture the current compile/test status before changing the runtime so regressions are attributable to the upgrade.
  - **Changes to Make**: Run baseline compilation and tests under the current Java 17 setup.
  - **Verification**: `mvn clean compile test-compile -q && mvn clean test -q` using the current JDK; expected result: baseline status recorded before the Java 25 upgrade.

- Step 3: Upgrade Java Runtime and Spring Boot Compatibility
  - **Rationale**: This is the required compatibility step for the Java 25 target and aligns the framework with the supported long-term line.
  - **Changes to Make**: Update `java.version` to 25 and upgrade `spring-boot-starter-parent` to the stable 3.5.x release in `pom.xml`.
  - **Verification**: `mvn clean test-compile -q` using Java 25; expected result: main and test sources compile successfully.

- Step 4: Fix Remaining Compatibility Issues
  - **Rationale**: Any build or test failures from the Java 25 transition must be fixed before the upgrade is considered complete.
  - **Changes to Make**: Apply only the minimal code/config changes required to satisfy the upgraded runtime and framework behavior.
  - **Verification**: `mvn clean test -q` using Java 25; expected result: all tests pass with the upgraded runtime.

- Step 5: Final Validation
  - **Rationale**: Confirm the final project state meets the success criteria after the upgrade.
  - **Changes to Make**: Complete any clean-up needed after the final fixes.
  - **Verification**: `mvn clean test -q` using Java 25; expected result: 100% pass rate under the target runtime.
