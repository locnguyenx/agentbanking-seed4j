# BDD Scenarios
## Seed4J Scaffolding Setup for Agent Banking Platform

**Version:** 1.0
**Date:** 2026-04-02
**Status:** Draft

---

## Happy Path Scenarios

### Scenario 1: Seed4J CLI Installation (Happy Path)
**US:** US-S01 | **FR:** FR-S01

**Given** Java 25 and Node.js are installed on the developer machine
**When** the developer clones and builds Seed4J CLI
**Then** the `seed4j` command is available in PATH
**And** `seed4j --version` displays the CLI version and Seed4J version

---

### Scenario 2: List Available Modules (Happy Path)
**US:** US-S01 | **FR:** FR-S01

**Given** Seed4J CLI is installed
**When** the developer runs `seed4j list`
**Then** a list of available modules is displayed
**And** the list includes `init`, `gradle-java`, `spring-boot`, `java-archunit` modules

---

### Scenario 3: Initialize Project with Gradle (Happy Path)
**US:** US-S03 | **FR:** FR-S05, FR-S11

**Given** Seed4J CLI is installed
**And** the developer is in an empty project directory
**When** the developer runs `seed4j apply init --project-name "Agent Banking" --base-name AgentBanking --node-package-manager npm`
**Then** the project is initialized with basic files (.gitignore, README.md, etc.)
**And** git repository is initialized

---

### Scenario 4: Setup Gradle Project Structure (Happy Path)
**US:** US-S03 | **FR:** FR-S05, FR-S11, FR-S12

**Given** the project is initialized
**When** the developer runs `seed4j apply gradle-java --package-name com.agentbanking`
**Then** a Gradle project structure is created with:
  - `build.gradle` with Java and Spring Boot plugins
  - `settings.gradle` for multi-project support
  - `src/main/java` and `src/test/java` directories
**And** no Maven files (pom.xml) are created

---

### Scenario 5: Add Spring Boot with Hexagonal Architecture (Happy Path)
**US:** US-S02 | **FR:** FR-S02, FR-S03, FR-S04

**Given** the Gradle project is set up
**When** the developer runs `seed4j apply spring-boot`
**Then** Spring Boot is configured in the project
**And** hexagonal architecture structure is created:
  - `domain/` package with model, port/in, port/out, service
  - `application/` package for use case orchestration
  - `infrastructure/` package with web, persistence adapters

---

### Scenario 6: Domain Layer Isolation Verification (Happy Path)
**US:** US-S06 | **FR:** FR-S09

**Given** a service has been generated with hexagonal architecture
**When** the developer runs `seed4j apply java-archunit`
**Then** ArchUnit tests are added to the project
**And** the tests verify that:
  - `domain/` package has no imports from `org.springframework.*`
  - `domain/` package has no imports from `jakarta.persistence.*`
  - `domain/` package has no imports from `org.apache.kafka.*`

---

### Scenario 7: Add SpringDoc OpenAPI (Happy Path)
**US:** US-S07 | **FR:** FR-S10

**Given** Spring Boot is configured
**When** the developer runs `seed4j apply springdoc-mvc-openapi`
**Then** SpringDoc OpenAPI dependency is added to build.gradle
**And** Swagger UI will be accessible at `/swagger-ui.html`
**And** OpenAPI spec will be available at `/v3/api-docs`

---

### Scenario 8: Add PostgreSQL Support (Happy Path)
**US:** US-S05 | **FR:** FR-S08

**Given** Spring Boot is configured
**When** the developer runs `seed4j apply datasource-postgresql` and `seed4j apply jpa-postgresql`
**Then** PostgreSQL datasource is configured
**And** JPA/Hibernate is configured
**And** database migration support (Flyway/Liquibase) is available

---

### Scenario 9: Build and Run Verification (Happy Path)
**US:** US-S02 | **FR:** FR-S02

**Given** all base modules are applied
**When** the developer runs `./gradlew build`
**Then** the build completes successfully
**And** all tests pass including ArchUnit tests

---

## Edge Cases

### Scenario 10: Missing Required Parameter (Edge Case)
**US:** US-S01 | **FR:** FR-S01

**Given** Seed4J CLI is installed
**When** the developer runs `seed4j apply gradle-java` without `--package-name`
**Then** the CLI displays an error message indicating the missing parameter
**And** the command exits with non-zero status

---

### Scenario 11: Domain Layer Violation Detection (Edge Case)
**US:** US-S06 | **FR:** FR-S09

**Given** ArchUnit tests are configured
**And** a developer accidentally adds a Spring import to the domain layer
**When** the tests are executed
**Then** the ArchUnit test fails with a clear message indicating the violation
**And** the build fails, preventing the violation from being committed

---

### Scenario 12: Module Dependency Missing (Edge Case)
**US:** US-S02 | **FR:** FR-S02

**Given** the project is not initialized
**When** the developer runs `seed4j apply spring-boot`
**Then** the CLI indicates that prerequisite modules are missing
**And** the developer is informed to run `init` and `gradle-java` first

---

## Traceability Matrix

| Scenario | US | FR | Type |
|----------|----|----|------|
| 1 | US-S01 | FR-S01 | Happy Path |
| 2 | US-S01 | FR-S01 | Happy Path |
| 3 | US-S03 | FR-S05, FR-S11 | Happy Path |
| 4 | US-S03 | FR-S05, FR-S11, FR-S12 | Happy Path |
| 5 | US-S02 | FR-S02, FR-S03, FR-S04 | Happy Path |
| 6 | US-S06 | FR-S09 | Happy Path |
| 7 | US-S07 | FR-S10 | Happy Path |
| 8 | US-S05 | FR-S08 | Happy Path |
| 9 | US-S02 | FR-S02 | Happy Path |
| 10 | US-S01 | FR-S01 | Edge Case |
| 11 | US-S06 | FR-S09 | Edge Case |
| 12 | US-S02 | FR-S02 | Edge Case |
