# Seed4J Scaffolding Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up Seed4J CLI and scaffold the Agent Banking Platform with hexagonal architecture using Gradle

**Architecture:** Install Seed4J CLI, initialize project with Gradle (not Maven), apply Seed4J modules for Spring Boot + hexagonal architecture, add ArchUnit enforcement, create Docker Compose infrastructure, and build common module with error handling

**Tech Stack:** Java 25, Spring Boot 4, Gradle, Seed4J CLI, PostgreSQL, Redis, Kafka, Keycloak, Axon Framework, ArchUnit, SpringDoc OpenAPI, Spring Cloud Gateway

---

## File Structure

```
agentbanking-seed4j/
├── settings.gradle                          # Root Gradle settings (Create)
├── build.gradle                             # Shared build config (Create)
├── gradlew / gradlew.bat                   # Gradle wrapper (Create via seed4j)
├── docker-compose.yml                       # Infrastructure (Create)
├── .seed4j/modules/history.json             # Seed4J tracking (Create via seed4j)
├── common/
│   ├── build.gradle                         # Common module build (Create)
│   └── src/main/java/com/agentbanking/common/
│       ├── error/
│       │   ├── GlobalError.java             # Error schema (Create)
│       │   ├── ErrorCategory.java           # Error categories (Create)
│       │   └── BusinessException.java       # Base exception (Create)
│       └── dto/
│           └── ApiResponse.java             # Standard response wrapper (Create)
├── rules-service/
│   ├── build.gradle                         # Service build (Create)
│   └── src/main/java/com/agentbanking/rules/
│       ├── domain/                          # Zero framework imports (Create)
│       ├── application/                     # Use case orchestration (Create)
│       └── infrastructure/                  # Adapters (Create)
├── ledger-service/                          # Same structure (Create)
├── onboarding-service/                      # Same structure (Create)
├── switch-adapter-service/                  # Same structure (Create)
├── biller-service/                          # Same structure (Create)
└── api-gateway/                             # Gateway service (Create via seed4j)

# Additional infrastructure (via Seed4J modules):
# - Kafka (seed4j apply kafka)
# - Flyway DB migrations (seed4j apply flyway, flyway-database-postgresql)
# - OAuth2/Keycloak (seed4j apply spring-boot-oauth2, spring-boot-oauth2-account)
# - Spring Cloud Gateway (seed4j apply spring-cloud-gateway)

# Manual setup required:
# - Axon Framework (CQRS/Event Sourcing) - no Seed4J module
# - Keycloak-specific config (realm, client ID, issuer URI)
```

---

## Prerequisites

Before starting these tasks, ensure:
- Java 25 is installed (`java -version`)
- Node.js is installed (`node -v`)
- Git is available (`git --version`)

---

### Task 1: Install Seed4J CLI

**BDD Scenarios:** Implements BDD Scenario 1: Seed4J CLI Installation

**BRD Requirements:** Fulfills US-S01: Install Seed4J CLI, FR-S01: Seed4J CLI accessible via command

**User-Facing:** NO

**Files:**
- Create: `/usr/local/bin/seed4j` (system binary)
- Create: `/usr/local/bin/seed4j.jar` (JAR file)

- [ ] **Step 1: Verify Java 25 and Node.js are installed**

Run: `java -version && node -v`
Expected: Java 25.x.x and Node.js v22+

- [ ] **Step 2: Clone Seed4J CLI repository**

Run:
```bash
cd /tmp
git clone https://github.com/seed4j/seed4j-cli
cd seed4j-cli
```

- [ ] **Step 3: Build Seed4J CLI**

Run: `./mvnw clean package`
Expected: BUILD SUCCESS, JAR file in `target/seed4j-cli-*.jar`

- [ ] **Step 4: Install Seed4J CLI to system path**

Run:
```bash
echo "java -jar \"/usr/local/bin/seed4j.jar\" \"\$@\"" | sudo tee /usr/local/bin/seed4j > /dev/null
sudo chmod +x /usr/local/bin/seed4j
JAR_SOURCE=$(ls target/seed4j-cli-*.jar | head -n 1)
sudo mv "$JAR_SOURCE" /usr/local/bin/seed4j.jar
```

- [ ] **Step 5: Verify installation**

Run: `seed4j --version`
Expected: Displays CLI version and Seed4J version

- [ ] **Step 6: List available modules**

Run: `seed4j list`
Expected: List of available modules including `init`, `gradle-java`, `spring-boot`

- [ ] **Step 7: Commit**

N/A - System installation, not project files

---

### Task 2: Initialize Project with Seed4J

**BDD Scenarios:** Implements BDD Scenario 3: Initialize Project with Gradle

**BRD Requirements:** Fulfills US-S03: Gradle monorepo structure, FR-S05: Gradle multi-project build

**User-Facing:** NO

**Files:**
- Create: `.gitignore`
- Create: `README.md`
- Create: `.editorconfig`
- Create: `.seed4j/modules/history.json`

- [ ] **Step 1: Navigate to project directory**

Run: `cd D:/Working/myprojects/agentbanking-seed4j`

- [ ] **Step 2: Initialize project with Seed4J**

Run:
```bash
seed4j apply init \
  --project-name "Agent Banking Platform" \
  --base-name AgentBanking \
  --node-package-manager npm
```

Expected: Creates .gitignore, README.md, .editorconfig

- [ ] **Step 3: Verify initialization**

Run: `ls -la`
Expected: .gitignore, README.md, .editorconfig present

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "chore: initialize project with Seed4J"
```

---

### Task 3: Setup Gradle Project Structure

**BDD Scenarios:** Implements BDD Scenario 4: Setup Gradle Project Structure

**BRD Requirements:** Fulfills US-S03: Gradle monorepo, FR-S05: Gradle multi-project, FR-S11: gradle-java module

**User-Facing:** NO

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradlew`, `gradlew.bat`
- Create: `src/main/java/com/agentbanking/` directory structure

- [ ] **Step 1: Write failing test for Gradle build**

Create `test-gradle.sh`:
```bash
#!/bin/bash
# Test that Gradle wrapper exists and build.gradle is configured for Java 25
[ -f "./gradlew" ] && [ -f "./build.gradle" ] && grep -q "VERSION_25" build.gradle
```

Run: `bash test-gradle.sh`
Expected: FAIL (files don't exist yet)

- [ ] **Step 2: Apply Gradle Java module**

Run:
```bash
seed4j apply gradle-java --package-name com.agentbanking
```

Expected: Creates build.gradle, settings.gradle, gradlew files

- [ ] **Step 3: Verify Gradle structure**

Run: `ls -la && cat build.gradle | head -20`
Expected: build.gradle with Java/Spring Boot plugins

- [ ] **Step 4: Run test to verify**

Run: `bash test-gradle.sh`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "build: setup Gradle project structure with Seed4J"
```

---

### Task 4: Add Java Base and Code Quality

**BDD Scenarios:** Implements BDD Scenario 9: Build and Run Verification

**BRD Requirements:** Fulfills FR-S06: Common build configuration

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/agentbanking/` base classes
- Create: `.prettierrc`, `.prettierignore`

- [ ] **Step 1: Add Java base module**

Run: `seed4j apply java-base`
Expected: Adds Java base classes and configuration

- [ ] **Step 2: Add code formatting**

Run: `seed4j apply prettier`
Expected: Adds Prettier configuration files

- [ ] **Step 3: Add Java utilities**

Run:
```bash
seed4j apply java-memoizers
seed4j apply java-enums
```

Expected: Adds memoizer and enum utilities

- [ ] **Step 4: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "build: add Java base classes and code quality tools"
```

---

### Task 5: Add Spring Boot with Hexagonal Architecture

**BDD Scenarios:** Implements BDD Scenario 5: Add Spring Boot with Hexagonal Architecture

**BRD Requirements:** Fulfills US-S02: Hexagonal architecture, FR-S02: domain/application/infrastructure layers

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/agentbanking/` hexagonal structure
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/agentbanking/` test structure

- [ ] **Step 1: Add Spring Boot module**

Run: `seed4j apply spring-boot`
Expected: Adds Spring Boot dependencies and configuration

- [ ] **Step 2: Add Tomcat server**

Run: `seed4j apply spring-boot-tomcat`
Expected: Configures embedded Tomcat

- [ ] **Step 3: Add Actuator for health checks**

Run: `seed4j apply spring-boot-actuator`
Expected: Adds actuator endpoints

- [ ] **Step 4: Add hexagonal architecture documentation**

Run: `seed4j apply application-service-hexagonal-architecture-documentation`
Expected: Creates domain/application/infrastructure structure

- [ ] **Step 5: Verify hexagonal structure**

Run: `find src -type d | head -20`
Expected: domain/, application/, infrastructure/ packages

- [ ] **Step 6: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: add Spring Boot with hexagonal architecture"
```

---

### Task 6: Add ArchUnit for Architecture Enforcement

**BDD Scenarios:** Implements BDD Scenario 6: Domain Layer Isolation Verification, BDD Scenario 11: Domain Layer Violation Detection

**BRD Requirements:** Fulfills US-S06: ArchUnit tests, FR-S09: Hexagonal architecture compliance tests

**User-Facing:** NO

**Files:**
- Create: `src/test/java/com/agentbanking/architecture/HexagonalArchitectureTest.java`

- [ ] **Step 1: Add ArchUnit module**

Run: `seed4j apply java-archunit`
Expected: Adds ArchUnit dependency and base tests

- [ ] **Step 2: Write domain isolation test**

Create `src/test/java/com/agentbanking/architecture/HexagonalArchitectureTest.java`:
```java
package com.agentbanking.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .importPackages("com.agentbanking");

    @Test
    void domain_should_not_depend_on_spring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
        rule.check(importedClasses);
    }

    @Test
    void domain_should_not_depend_on_kafka() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.apache.kafka..");
        rule.check(importedClasses);
    }

    @Test
    void controllers_should_not_expose_entities() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence.entity..");
        rule.check(importedClasses);
    }
}
```

- [ ] **Step 3: Run ArchUnit tests**

Run: `./gradlew test --tests "*HexagonalArchitectureTest*"`
Expected: PASS (if hexagonal structure is correct)

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/agentbanking/architecture/
git commit -m "test: add ArchUnit tests for hexagonal architecture enforcement"
```

---

### Task 7: Add PostgreSQL, JPA, and Flyway Support

**BDD Scenarios:** Implements BDD Scenario 8: Add PostgreSQL Support

**BRD Requirements:** Fulfills US-S05: Docker Compose infrastructure, FR-S08: PostgreSQL, Redis, Kafka, Keycloak

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/db/migration/` (Flyway)

- [ ] **Step 1: Add PostgreSQL datasource**

Run: `seed4j apply datasource-postgresql`
Expected: Adds PostgreSQL configuration

- [ ] **Step 2: Add JPA support**

Run: `seed4j apply jpa-postgresql`
Expected: Adds JPA/Hibernate dependencies

- [ ] **Step 3: Add Flyway for migrations**

Run:
```bash
seed4j apply flyway
seed4j apply flyway-database-postgresql
```

Expected: Adds Flyway configuration and PostgreSQL-specific Flyway support

- [ ] **Step 4: Verify database configuration**

Run: `cat src/main/resources/application.yml | grep -A 5 "datasource"`
Expected: PostgreSQL datasource configuration

Run: `ls src/main/resources/db/migration/`
Expected: Flyway migration directory exists

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "build: add PostgreSQL, JPA, and Flyway migration support"
```

---

### Task 8: Add Kafka Messaging

**BDD Scenarios:** Implements BDD Scenario 8: Add PostgreSQL Support (infrastructure)

**BRD Requirements:** Fulfills FR-S08: Kafka infrastructure

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: Kafka Docker Compose (via Seed4J)

- [ ] **Step 1: Add Kafka module**

Run: `seed4j apply kafka`
Expected: Adds Kafka dependencies, Docker Compose, producer/consumer config

- [ ] **Step 2: Verify Kafka configuration**

Run: `cat src/main/resources/application.yml | grep -A 5 "kafka"`
Expected: Kafka broker configuration

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "build: add Kafka messaging support"
```

---

### Task 9: Add Spring Cloud Gateway

**BDD Scenarios:** Implements BDD Scenario 7: Add SpringDoc OpenAPI (infrastructure)

**BRD Requirements:** Fulfills US-S07: API Gateway service

**User-Facing:** NO

**Files:**
- Create: `api-gateway/` service (via Seed4J)
- Modify: `settings.gradle`

- [ ] **Step 1: Add Spring Cloud Gateway module**

Run: `seed4j apply spring-cloud-gateway`
Expected: Creates API Gateway service with routing configuration

- [ ] **Step 2: Verify gateway structure**

Run: `ls -la api-gateway/`
Expected: Gateway service with build.gradle and configuration

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add Spring Cloud Gateway service"
```

---

### Task 10: Add Keycloak IAM (OAuth2)

**BDD Scenarios:** Implements BDD Scenario 8: Add PostgreSQL Support (infrastructure)

**BRD Requirements:** Fulfills FR-S08: Keycloak IAM

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: OAuth2 configuration (via Seed4J)

- [ ] **Step 1: Add OAuth2 module**

Run:
```bash
seed4j apply spring-boot-oauth2
seed4j apply spring-boot-oauth2-account
```

Expected: Adds OAuth2 dependencies and security configuration

- [ ] **Step 2: Add Keycloak-specific configuration**

Add to `src/main/resources/application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9080/realms/agentbanking
```

- [ ] **Step 3: Verify OAuth2 configuration**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "build: add Keycloak IAM with OAuth2 support"
```

---

### Task 11: Add OpenAPI Documentation

**BDD Scenarios:** Implements BDD Scenario 7: Add SpringDoc OpenAPI

**BRD Requirements:** Fulfills US-S07: SpringDoc OpenAPI, FR-S10: API documentation

**User-Facing:** NO

**Files:**
- Modify: `build.gradle`
- Create: Swagger configuration class

- [ ] **Step 1: Add SpringDoc OpenAPI module**

Run: `seed4j apply springdoc-mvc-openapi`
Expected: Adds SpringDoc dependency

- [ ] **Step 2: Verify OpenAPI configuration**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "build: add SpringDoc OpenAPI for API documentation"
```

---

### Task 12: Create Docker Compose Infrastructure

**BDD Scenarios:** Implements BDD Scenario 8: Add PostgreSQL Support

**BRD Requirements:** Fulfills US-S05: Docker Compose, FR-S08: Infrastructure services

**User-Facing:** NO

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Create docker-compose.yml**

Create `docker-compose.yml`:
```yaml
services:
  postgres:
    image: postgres:16
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: agentbanking
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    ports: ["9080:8080"]
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin

  axon-server:
    image: axoniq/axonserver:2024.1.3
    ports:
      - "8024:8024"
      - "8124:8124"
    volumes:
      - axonserver-data:/axonserver/data
      - axonserver-events:/axonserver/events

volumes:
  postgres-data:
  axonserver-data:
  axonserver-events:
```

- [ ] **Step 2: Verify Docker Compose syntax**

Run: `docker compose config`
Expected: Valid configuration output

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yml
git commit -m "build: add Docker Compose for PostgreSQL, Redis, Kafka, Keycloak"
```

---

### Task 13: Create Common Module with Error Handling

**BDD Scenarios:** Implements BDD Scenario 9: Build and Run Verification

**BRD Requirements:** Fulfills US-S04: Common module, FR-S07: GlobalError schema

**User-Facing:** NO

**Files:**
- Create: `common/build.gradle`
- Create: `common/src/main/java/com/agentbanking/common/error/GlobalError.java`
- Create: `common/src/main/java/com/agentbanking/common/error/ErrorCategory.java`
- Create: `common/src/main/java/com/agentbanking/common/error/BusinessException.java`
- Modify: `settings.gradle`

- [ ] **Step 1: Update settings.gradle for common module**

Add to `settings.gradle`:
```groovy
rootProject.name = 'agentbanking'

include 'common'
```

- [ ] **Step 2: Create common module build.gradle**

Create `common/build.gradle`:
```groovy
plugins {
    id 'java-library'
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
```

- [ ] **Step 3: Create GlobalError record**

Create `common/src/main/java/com/agentbanking/common/error/GlobalError.java`:
```java
package com.agentbanking.common.error;

import java.time.OffsetDateTime;

public record GlobalError(
    String status,
    ErrorDetail error,
    String traceId,
    OffsetDateTime timestamp
) {
    public record ErrorDetail(
        String code,
        String message,
        String actionCode,
        String traceId,
        OffsetDateTime timestamp
    ) {}

    public static GlobalError of(String code, String message, String actionCode, String traceId) {
        var now = OffsetDateTime.now();
        return new GlobalError(
            "FAILED",
            new ErrorDetail(code, message, actionCode, traceId, now),
            traceId,
            now
        );
    }
}
```

- [ ] **Step 4: Create ErrorCategory enum**

Create `common/src/main/java/com/agentbanking/common/error/ErrorCategory.java`:
```java
package com.agentbanking.common.error;

public enum ErrorCategory {
    ERR_VAL,    // Validation errors
    ERR_BIZ,    // Business logic errors
    ERR_EXT,    // External system errors
    ERR_AUTH,   // Authentication/authorization errors
    ERR_SYS     // System errors
}
```

- [ ] **Step 5: Create BusinessException**

Create `common/src/main/java/com/agentbanking/common/error/BusinessException.java`:
```java
package com.agentbanking.common.error;

public class BusinessException extends RuntimeException {
    private final String code;
    private final String actionCode;

    public BusinessException(String code, String message, String actionCode) {
        super(message);
        this.code = code;
        this.actionCode = actionCode;
    }

    public String getCode() { return code; }
    public String getActionCode() { return actionCode; }
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add common/ settings.gradle
git commit -m "feat: add common module with GlobalError schema"
```

---

### Task 14: Manual Axon Framework Setup

**BDD Scenarios:** N/A (manual setup, no Seed4J module)

**BRD Requirements:** Fulfills US-S02: CQRS/Event Sourcing architecture

**User-Facing:** NO

**Files:**
- Modify: `build.gradle` (root and service modules)
- Modify: `src/main/resources/application.yml`
- Create: Axon configuration classes

- [ ] **Step 1: Add Axon dependencies to build.gradle**

Add to root `build.gradle`:
```groovy
ext {
    axonVersion = '4.11.0'
}

subprojects {
    dependencies {
        implementation "org.axonframework:axon-spring-boot-starter:${axonVersion}"
        implementation "org.axonframework:axon-server-connector:${axonVersion}"
        testImplementation "org.axonframework:axon-test:${axonVersion}"
    }
}
```

- [ ] **Step 2: Configure Axon Server connection**

Add to `src/main/resources/application.yml`:
```yaml
axon:
  axonserver:
    servers: localhost:8124
```

- [ ] **Step 3: Verify Axon configuration**

Run: `./gradlew build`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add build.gradle src/main/resources/application.yml
git commit -m "build: add Axon Framework for CQRS/Event Sourcing"
```

---

### Task 15: Verify Complete Build

**BDD Scenarios:** Implements BDD Scenario 9: Build and Run Verification

**BRD Requirements:** Fulfills NFR-S02: All services start with ./gradlew bootRun

**User-Facing:** NO

**Files:**
- N/A (verification only)

- [ ] **Step 1: Clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

- [ ] **Step 3: Verify ArchUnit tests**

Run: `./gradlew test --tests "*Architecture*"`
Expected: All architecture tests PASS

- [ ] **Step 4: Start infrastructure**

Run: `docker compose up -d`
Expected: All containers started

- [ ] **Step 5: Verify application starts**

Run: `./gradlew bootRun`
Expected: Application starts on port 8080

---

## Summary

| Task | Description | Type | BRD Requirements |
|------|-------------|------|------------------|
| 1 | Install Seed4J CLI | Manual | US-S01, FR-S01 |
| 2 | Initialize Project | Seed4J | US-S03, FR-S05 |
| 3 | Setup Gradle Structure | Seed4J | US-S03, FR-S05, FR-S11 |
| 4 | Add Java Base + Quality | Seed4J | FR-S06 |
| 5 | Add Spring Boot + Hexagonal | Seed4J | US-S02, FR-S02 |
| 6 | Add ArchUnit | Seed4J | US-S06, FR-S09 |
| 7 | Add PostgreSQL + JPA + Flyway | Seed4J | US-S05, FR-S08 |
| 8 | Add Kafka Messaging | Seed4J | FR-S08 |
| 9 | Add Spring Cloud Gateway | Seed4J | US-S07 |
| 10 | Add Keycloak IAM (OAuth2) | Seed4J + Manual | FR-S08 |
| 11 | Add OpenAPI Documentation | Seed4J | US-S07, FR-S10 |
| 12 | Create Docker Compose | Manual | US-S05, FR-S08 |
| 13 | Create Common Module | Manual | US-S04, FR-S07 |
| 14 | Manual Axon Framework Setup | Manual | US-S02 |
| 15 | Verify Complete Build | Manual | NFR-S02 |
