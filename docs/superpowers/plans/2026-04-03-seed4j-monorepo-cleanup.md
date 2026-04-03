# Seed4J Monorepo Cleanup and Regeneration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clean up incorrectly scaffolded project structure and regenerate all microservices using Seed4J CLI with correct module application per service, following the monorepo pattern documented in `docs/superpowers/guides/seed4j-monorepo-guidelines.md`.

**Architecture:** Monorepo with 6 independent Seed4J projects (1 API Gateway + 5 domain services). Each service has its own `.seed4j/` directory and module history. Root is a lightweight Gradle parent only. Gateway uses WebFlux stack, domain services use Tomcat (MVC) stack.

**Tech Stack:** Java 21, Spring Boot 4, Gradle (Kotlin DSL), Seed4J CLI, PostgreSQL, Redis, Kafka, Spring Cloud Gateway, ArchUnit

---

## File Structure

### Files to DELETE
- `src/` - Root source directory (mixed concerns, should not exist)
- `.seed4j/` - Root Seed4J modules (should not exist at monorepo root)
- `api-gateway-service/` - Manually created, will regenerate with Seed4J
- All 5 service `.seed4j/` directories - Will regenerate with correct modules
- All 5 service `src/` directories - Will regenerate with correct structure

### Files to MODIFY
- `settings.gradle.kts` - Remove `api-gateway-service`, keep 5 services
- `build.gradle.kts` - Make lightweight parent (remove all implementation dependencies)
- `gradle/libs.versions.toml` - Remove gateway-specific entries (will be per-service)

### Files to CREATE
- `api-gateway-service/.seed4j/modules/history.json` - Via Seed4J
- `api-gateway-service/build.gradle.kts` - Via Seed4J (WebFlux stack)
- `api-gateway-service/src/` - Via Seed4J
- `rules-service/.seed4j/modules/history.json` - Via Seed4J
- `rules-service/build.gradle.kts` - Via Seed4J (Tomcat stack)
- `rules-service/src/` - Via Seed4J
- (Same pattern for all 5 domain services)
- `common/build.gradle.kts` - Manual Java library
- `common/src/main/java/com/agentbanking/common/error/GlobalError.java` - Manual
- `common/src/main/java/com/agentbanking/common/error/ErrorCategory.java` - Manual
- `common/src/main/java/com/agentbanking/common/error/BusinessException.java` - Manual

---

### Task 1: Install Seed4J CLI

**BDD Scenarios:** Implements BDD Scenario 1: Seed4J CLI Installation

**BRD Requirements:** Fulfills US-S01: Install Seed4J CLI, FR-S01: Seed4J CLI accessible via command

**User-Facing:** NO

**Files:**
- System: `/usr/local/bin/seed4j` (system binary)
- System: `/usr/local/bin/seed4j.jar` (JAR file)

- [ ] **Step 1: Verify Java 21 and Node.js are installed**

Run: `java -version && node -v`
Expected: Java 21.x.x and Node.js v22+

- [ ] **Step 2: Check if Seed4J CLI is already installed**

Run: `seed4j --version`
If not found, proceed to Step 3.

- [ ] **Step 3: Build Seed4J CLI from source**

Run:
```bash
cd /tmp/seed4j-cli
./mvnw clean package -DskipTests
```
Expected: BUILD SUCCESS, JAR file in `target/seed4j-cli-0.0.1-SNAPSHOT.jar`

- [ ] **Step 4: Install Seed4J CLI to system path**

Run:
```bash
echo "java -jar \"/usr/local/bin/seed4j.jar\" \"\$@\"" | sudo tee /usr/local/bin/seed4j > /dev/null
sudo chmod +x /usr/local/bin/seed4j
JAR_SOURCE=$(ls /tmp/seed4j-cli/target/seed4j-cli-*.jar | head -n 1)
sudo mv "$JAR_SOURCE" /usr/local/bin/seed4j.jar
```

- [ ] **Step 5: Verify installation**

Run: `seed4j --version`
Expected: Displays CLI version and Seed4J version

- [ ] **Step 6: List available modules**

Run: `seed4j list`
Expected: List of available modules including `init`, `gradle-java`, `spring-boot`

---

### Task 2: Clean Up Root Project Structure

**BDD Scenarios:** Implements BDD Scenario 3: Initialize Project with Gradle

**BRD Requirements:** Fulfills US-S03: Gradle monorepo structure, FR-S05: Gradle multi-project build

**User-Facing:** NO

**Files:**
- Delete: `src/`
- Delete: `.seed4j/`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`

- [ ] **Step 1: Delete root src/ directory**

Run:
```bash
rm -rf src/
```
Expected: Root `src/` removed

- [ ] **Step 2: Delete root .seed4j/ directory**

Run:
```bash
rm -rf .seed4j/
```
Expected: Root `.seed4j/` removed

- [ ] **Step 3: Rewrite settings.gradle.kts as lightweight parent**

Replace `settings.gradle.kts` with:
```kotlin
rootProject.name = "agent-banking"

include("api-gateway-service")
include("rules-service")
include("ledger-service")
include("onboarding-service")
include("switch-adapter-service")
include("biller-service")
include("common")
```

- [ ] **Step 4: Rewrite build.gradle.kts as lightweight parent**

Replace `build.gradle.kts` with:
```kotlin
plugins {
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

// No implementations at root level - only dependency management
// Each service manages its own dependencies
```

- [ ] **Step 5: Clean up gradle/libs.versions.toml**

Remove gateway-specific entries that don't belong at root level. Keep only shared versions.

- [ ] **Step 6: Verify Gradle structure**

Run: `./gradlew projects`
Expected: Lists all 7 subprojects (including common)

---

### Task 3: Delete and Regenerate API Gateway Service (WebFlux)

**BDD Scenarios:** Implements BDD Scenario 5: Add Spring Boot with Hexagonal Architecture

**BRD Requirements:** Fulfills US-S07: API Gateway service

**User-Facing:** NO

**Files:**
- Delete: `api-gateway-service/` (entire directory)
- Create: `api-gateway-service/.seed4j/modules/history.json`
- Create: `api-gateway-service/build.gradle.kts`
- Create: `api-gateway-service/src/main/java/com/agentbanking/gateway/`
- Create: `api-gateway-service/src/test/java/com/agentbanking/gateway/`
- Create: `api-gateway-service/src/main/resources/`

- [ ] **Step 1: Delete existing api-gateway-service/**

Run:
```bash
rm -rf api-gateway-service/
```

- [ ] **Step 2: Create directory and initialize with Seed4J**

Run:
```bash
mkdir -p api-gateway-service
seed4j apply init \
  --project-path ./api-gateway-service \
  --project-name "API Gateway" \
  --base-name ApiGateway \
  --package-name com.agentbanking.gateway \
  --node-package-manager npm \
  --no-commit
```

- [ ] **Step 3: Apply Gradle setup**

Run:
```bash
seed4j apply gradle-java \
  --project-path ./api-gateway-service \
  --package-name com.agentbanking.gateway \
  --no-commit

seed4j apply gradle-wrapper \
  --project-path ./api-gateway-service \
  --no-commit
```

- [ ] **Step 4: Apply Java base and quality**

Run:
```bash
seed4j apply java-base --project-path ./api-gateway-service --no-commit
seed4j apply prettier --project-path ./api-gateway-service --no-commit
```

- [ ] **Step 5: Apply Spring Boot with WebFlux (NOT Tomcat)**

Run:
```bash
seed4j apply spring-boot \
  --project-path ./api-gateway-service \
  --server-port 8080 \
  --no-commit

seed4j apply spring-boot-webflux-empty --project-path ./api-gateway-service --no-commit
seed4j apply spring-cloud --project-path ./api-gateway-service --no-commit
seed4j apply gateway \
  --project-path ./api-gateway-service \
  --package-name com.agentbanking.gateway \
  --base-name ApiGateway \
  --no-commit
```

- [ ] **Step 6: Apply cross-cutting modules**

Run:
```bash
seed4j apply spring-boot-actuator --project-path ./api-gateway-service --no-commit
seed4j apply java-archunit --project-path ./api-gateway-service --no-commit
seed4j apply logs-spy --project-path ./api-gateway-service --no-commit
seed4j apply springdoc-webflux-openapi --project-path ./api-gateway-service --no-commit
```

- [ ] **Step 7: Verify build**

Run:
```bash
cd api-gateway-service
./gradlew build -x test
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

Run:
```bash
cd ..
git add api-gateway-service/
git commit -m "feat(gateway): scaffold API Gateway with Spring Cloud Gateway (WebFlux)"
```

---

### Task 4: Regenerate Domain Services (Tomcat)

**BDD Scenarios:** Implements BDD Scenario 5: Add Spring Boot with Hexagonal Architecture, BDD Scenario 6: Domain Layer Isolation Verification

**BRD Requirements:** Fulfills US-S02: Hexagonal architecture, FR-S02: domain/application/infrastructure layers, US-S06: ArchUnit tests

**User-Facing:** NO

**Files:**
- Delete: Each service's `.seed4j/`, `src/`, `build.gradle.kts`, `gradle/`
- Create: All regenerated via Seed4J

For each service, apply this template:

```bash
# Phase 1: Foundation
seed4j apply init \
  --project-path ./<service> \
  --project-name "Agent Banking <ServiceName>" \
  --base-name AgentBanking<ServiceName> \
  --package-name com.agentbanking.<package> \
  --node-package-manager npm \
  --no-commit

seed4j apply gradle-java \
  --project-path ./<service> \
  --package-name com.agentbanking.<package> \
  --no-commit

seed4j apply gradle-wrapper --project-path ./<service> --no-commit
seed4j apply java-base --project-path ./<service> --no-commit
seed4j apply prettier --project-path ./<service> --no-commit
seed4j apply java-memoizers --project-path ./<service> --no-commit
seed4j apply java-enums --project-path ./<service> --no-commit

# Phase 2: Server core (Tomcat - servlet stack)
seed4j apply spring-boot \
  --project-path ./<service> \
  --server-port <port> \
  --no-commit

seed4j apply spring-boot-tomcat --project-path ./<service> --no-commit
seed4j apply spring-boot-actuator --project-path ./<service> --no-commit
seed4j apply application-service-hexagonal-architecture-documentation --project-path ./<service> --no-commit
seed4j apply java-archunit --project-path ./<service> --no-commit

# Phase 3: Database
seed4j apply datasource-postgresql --project-path ./<service> --no-commit
seed4j apply jpa-postgresql --project-path ./<service> --no-commit
seed4j apply flyway --project-path ./<service> --no-commit
seed4j apply flyway-postgresql --project-path ./<service> --no-commit

# Phase 4: Messaging and docs
seed4j apply spring-boot-kafka --project-path ./<service> --no-commit
seed4j apply springdoc-mvc-openapi --project-path ./<service> --no-commit
seed4j apply logs-spy --project-path ./<service> --no-commit
```

- [ ] **Step 1: Regenerate Rules Service** (port 8081, package: `com.agentbanking.rules`)

Delete existing:
```bash
rm -rf rules-service/.seed4j/ rules-service/src/ rules-service/build.gradle.kts rules-service/gradle/
```

Apply modules using template above with:
- `<service>` = `rules-service`
- `<ServiceName>` = `Rules`
- `<package>` = `rules`
- `<port>` = `8081`

- [ ] **Step 2: Regenerate Ledger Service** (port 8082, package: `com.agentbanking.ledger`)

Delete existing:
```bash
rm -rf ledger-service/.seed4j/ ledger-service/src/ ledger-service/build.gradle.kts ledger-service/gradle/
```

Apply modules using template above with:
- `<service>` = `ledger-service`
- `<ServiceName>` = `Ledger`
- `<package>` = `ledger`
- `<port>` = `8082`

- [ ] **Step 3: Regenerate Onboarding Service** (port 8083, package: `com.agentbanking.onboarding`)

Delete existing:
```bash
rm -rf onboarding-service/.seed4j/ onboarding-service/src/ onboarding-service/build.gradle.kts onboarding-service/gradle/
```

Apply modules using template above with:
- `<service>` = `onboarding-service`
- `<ServiceName>` = `Onboarding`
- `<package>` = `onboarding`
- `<port>` = `8083`

- [ ] **Step 4: Regenerate Switch Adapter Service** (port 8084, package: `com.agentbanking.switchadapter`)

Delete existing:
```bash
rm -rf switch-adapter-service/.seed4j/ switch-adapter-service/src/ switch-adapter-service/build.gradle.kts switch-adapter-service/gradle/
```

Apply modules using template above with:
- `<service>` = `switch-adapter-service`
- `<ServiceName>` = `SwitchAdapter`
- `<package>` = `switchadapter`
- `<port>` = `8084`

- [ ] **Step 5: Regenerate Biller Service** (port 8085, package: `com.agentbanking.biller`)

Delete existing:
```bash
rm -rf biller-service/.seed4j/ biller-service/src/ biller-service/build.gradle.kts biller-service/gradle/
```

Apply modules using template above with:
- `<service>` = `biller-service`
- `<ServiceName>` = `Biller`
- `<package>` = `biller`
- `<port>` = `8085`

- [ ] **Step 6: Verify all services build**

Run:
```bash
for svc in api-gateway-service rules-service ledger-service onboarding-service switch-adapter-service biller-service; do
  echo "=== Building $svc ==="
  cd $svc && ./gradlew build -x test && cd ..
done
```
Expected: All 6 services BUILD SUCCESS

- [ ] **Step 7: Commit all services**

Run:
```bash
git add rules-service/ ledger-service/ onboarding-service/ switch-adapter-service/ biller-service/
git commit -m "feat: regenerate 5 domain services with Seed4J (Tomcat stack)"
```

---

### Task 5: Create Common Module (MANUAL)

**BDD Scenarios:** Implements BDD Scenario 9: Build and Run Verification

**BRD Requirements:** Fulfills US-S04: Common module, FR-S07: GlobalError schema

**User-Facing:** NO

**Files:**
- Create: `common/build.gradle.kts`
- Create: `common/src/main/java/com/agentbanking/common/error/GlobalError.java`
- Create: `common/src/main/java/com/agentbanking/common/error/ErrorCategory.java`
- Create: `common/src/main/java/com/agentbanking/common/error/BusinessException.java`

- [ ] **Step 1: Create common module build.gradle.kts**

```kotlin
plugins {
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

group = "com.agentbanking.common"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}
```

- [ ] **Step 2: Create GlobalError record**

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

- [ ] **Step 3: Create ErrorCategory enum**

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

- [ ] **Step 4: Create BusinessException**

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

- [ ] **Step 5: Verify build**

Run: `./gradlew :common:build`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

Run:
```bash
git add common/
git commit -m "feat: add common module with GlobalError schema"
```

---

### Task 6: Final Verification

**BDD Scenarios:** Implements BDD Scenario 9: Build and Run Verification

**BRD Requirements:** Fulfills NFR-S02: All services start with ./gradlew bootRun

**User-Facing:** NO

**Files:**
- N/A (verification only)

- [ ] **Step 1: Clean build all services**

Run: `./gradlew clean build -x test`
Expected: BUILD SUCCESS for all 7 projects

- [ ] **Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

- [ ] **Step 3: Run ArchUnit tests**

Run: `./gradlew test --tests "*ArchTest*"`
Expected: All architecture tests PASS

- [ ] **Step 4: Verify each service starts**

Run each in separate terminal:
```bash
./gradlew :api-gateway-service:bootRun
./gradlew :rules-service:bootRun
./gradlew :ledger-service:bootRun
./gradlew :onboarding-service:bootRun
./gradlew :switch-adapter-service:bootRun
./gradlew :biller-service:bootRun
```

- [ ] **Step 5: Commit**

Run:
```bash
git add -A
git commit -m "feat: complete MVP microservices scaffolding with Seed4J CLI"
```

---

## Summary

| Task | Description | Type | Services |
|------|-------------|------|----------|
| 1 | Install Seed4J CLI | Manual | N/A |
| 2 | Clean up root structure | Manual | N/A |
| 3 | Regenerate API Gateway | Seed4J | api-gateway-service |
| 4 | Regenerate Domain Services | Seed4J | 5 services |
| 5 | Create Common Module | Manual | N/A |
| 6 | Final Verification | Manual | All |
