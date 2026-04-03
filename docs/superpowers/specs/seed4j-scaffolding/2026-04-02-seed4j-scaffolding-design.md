# Design Specification
## Seed4J Scaffolding Setup for Agent Banking Platform

**Version:** 1.0
**Date:** 2026-04-02
**Status:** Draft

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Banking Platform                        │
├─────────────────────────────────────────────────────────────────┤
│  Tier 1: Channel Layer (POS Terminals)                         │
│  └─ Android/Flutter POS → REST/HTTPS                           │
├─────────────────────────────────────────────────────────────────┤
│  Tier 2: API Gateway (Spring Cloud Gateway)                    │
│  └─ JWT validation, rate limiting, routing                     │
├─────────────────────────────────────────────────────────────────┤
│  Tier 3: Business Core Services                                │
│  ├─ Rules Service (fees, limits, velocity)                     │
│  ├─ Ledger & Float Service (agent wallets)                     │
│  ├─ Onboarding Service (e-KYC, MyKad)                         │
│  ├─ Switch Adapter Service (ISO 8583/20022)                    │
│  ├─ Biller Service (utility payments)                          │
│  └─ Common Module (shared DTOs, errors)                        │
├─────────────────────────────────────────────────────────────────┤
│  Tier 4: Translation Layer (Future)                            │
│  └─ ISO Translation Engine, CBS Connector, HSM Wrapper         │
├─────────────────────────────────────────────────────────────────┤
│  Tier 5: Downstream Systems (Future)                           │
│  └─ PayNet, Core Banking, JPN, Billers                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Monorepo Project Structure

```
agentbanking-seed4j/
├── settings.gradle                    # Root Gradle settings
├── build.gradle                       # Shared build configuration
├── gradlew / gradlew.bat             # Gradle wrapper
├── docker-compose.yml                 # Infrastructure services
├── common/
│   ├── build.gradle
│   └── src/main/java/com/agentbanking/common/
│       ├── error/                     # GlobalError, error codes
│       ├── dto/                       # Shared DTOs
│       └── validation/                # Validation utilities
├── rules-service/
│   ├── build.gradle
│   └── src/main/java/com/agentbanking/rules/
│       ├── domain/
│       │   ├── model/                 # FeeConfig, VelocityRule
│       │   ├── port/
│       │   │   ├── in/                # Inbound ports (use cases)
│       │   │   └── out/               # Outbound ports (repository, gateway)
│       │   └── service/               # Business rules
│       ├── application/               # Use case orchestration
│       ├── infrastructure/
│       │   ├── web/                   # REST controllers
│       │   ├── persistence/           # JPA repositories
│       │   └── config/                # Spring configuration
│       └── DomainServiceConfig.java   # Bean registration
├── ledger-service/
│   └── (same hexagonal structure)
├── onboarding-service/
│   └── (same hexagonal structure)
├── switch-adapter-service/
│   └── (same hexagonal structure)
├── biller-service/
│   └── (same hexagonal structure)
└── api-gateway/
    └── (Spring Cloud Gateway configuration)
```

---

## 3. Seed4J CLI Scaffolding Workflow (Primary)

### Step 1: Install Seed4J CLI
```bash
# Clone and build Seed4J CLI
git clone https://github.com/seed4j/seed4j-cli
cd seed4j-cli

# Requires Java 25 and Node.js
./mvnw clean package

# Install to system path
echo "java -jar \"/usr/local/bin/seed4j.jar\" \"\$@\"" | sudo tee /usr/local/bin/seed4j > /dev/null
sudo chmod +x /usr/local/bin/seed4j
JAR_SOURCE=$(ls target/seed4j-cli-*.jar | head -n 1)
sudo mv "$JAR_SOURCE" /usr/local/bin/seed4j.jar

# Verify installation
seed4j --version
seed4j list
```

### Step 2: Initialize Project
```bash
cd agentbanking-seed4j
seed4j apply init \
  --project-name "Agent Banking Platform" \
  --base-name AgentBanking \
  --node-package-manager npm
```

### Step 3: Setup Gradle Project (NOT Maven)
```bash
seed4j apply gradle-java --package-name com.agentbanking
```

### Step 4: Add Core Modules
```bash
# Java base
seed4j apply java-base

# Code quality
seed4j apply prettier
seed4j apply java-memoizers
seed4j apply java-enums

# Architecture enforcement
seed4j apply java-archunit

# Spring Boot
seed4j apply spring-boot
seed4j apply spring-boot-tomcat
seed4j apply spring-boot-actuator

# Database
seed4j apply datasource-postgresql
seed4j apply jpa-postgresql
seed4j apply flyway
seed4j apply flyway-postgresql

# Kafka messaging
seed4j apply spring-boot-kafka

# Spring Cloud Gateway
seed4j apply gateway

# Keycloak IAM (OAuth2)
seed4j apply spring-boot-oauth2
seed4j apply spring-boot-oauth2-account

# API Documentation
seed4j apply springdoc-mvc-openapi
seed4j apply springdoc-oauth2
```

---

## 4. Seed4J REST API Scaffolding (Fallback)

If CLI is not available, use REST API against running Seed4J instance:

### Start Seed4J via Docker
```bash
docker run --rm -p 1339:1339 -v $(pwd):/tmp/seed4j seed4j/seed4j:latest
```

### Apply Modules via REST API
```bash
# Health check
curl http://localhost:1339/management/health

# Apply modules
curl -X POST http://localhost:1339/api/modules/init/apply-patch \
  -H "Content-Type: application/json" \
  -d '{"basePackage": "com.agentbanking", "projectFolder": "/tmp/seed4j"}'

curl -X POST http://localhost:1339/api/modules/gradle-java/apply-patch \
  -d '{"packageName": "com.agentbanking"}'

curl -X POST http://localhost:1339/api/modules/java-base/apply-patch
curl -X POST http://localhost:1339/api/modules/spring-boot/apply-patch
curl -X POST http://localhost:1339/api/modules/java-archunit/apply-patch
```

---

## 5. Architecture Enforcement via ArchUnit

Seed4J generates ArchUnit tests. We extend them for banking-specific rules:

```java
@AnalyzeClasses(packages = "com.agentbanking")
public class HexagonalArchitectureTest {

    @Test
    void domain_should_not_depend_on_spring() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .check(importedClasses);
    }

    @Test
    void domain_should_not_depend_on_kafka() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.apache.kafka..")
            .check(importedClasses);
    }

    @Test
    void controllers_should_not_expose_entities() {
        noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence.entity..")
            .check(importedClasses);
    }
}
```

---

## 6. Error Handling Design (Common Module)

```java
// Global Error Schema
public record GlobalError(
    String status,           // "FAILED"
    ErrorDetail error,
    String traceId,
    OffsetDateTime timestamp
) {
    public record ErrorDetail(
        String code,         // "ERR_BIZ_001"
        String message,      // "Insufficient funds"
        String actionCode,   // "DECLINE", "RETRY", "REVIEW"
        String traceId,
        OffsetDateTime timestamp
    ) {}
}

// Error Code Categories
public enum ErrorCategory {
    ERR_VAL,    // Validation errors
    ERR_BIZ,    // Business logic errors
    ERR_EXT,    // External system errors
    ERR_AUTH,   // Authentication/authorization errors
    ERR_SYS     // System errors
}
```

---

## 7. Infrastructure Design (docker-compose.yml)

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

volumes:
  postgres-data:
```

---

## 8. Data Flow Design

```
POS Terminal → API Gateway → Orchestrator → Rules Service (check limits)
                                   ↓
                            Ledger Service (block float)
                                   ↓
                            Switch Adapter → Tier 4 (future)
                                   ↓
                            Ledger Service (commit)
                                   ↓
                            Response to POS Terminal
```

---

## 9. Build Configuration (Gradle)

### settings.gradle
```groovy
rootProject.name = 'agentbanking'

include 'common'
include 'rules-service'
include 'ledger-service'
include 'onboarding-service'
include 'switch-adapter-service'
include 'biller-service'
include 'api-gateway'
```

### build.gradle (root)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0' apply false
    id 'io.spring.dependency-management' version '1.1.4'
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.1'
    }
}
```

---

## 10. Seed4J Module Mapping

| Our Need | Seed4J Module | Notes |
|----------|---------------|-------|
| Project init | `init` | Creates .gitignore, README |
| Gradle setup | `gradle-java` | NOT maven-java |
| Java base | `java-base` | Core Java classes |
| Spring Boot | `spring-boot` | Framework setup |
| Architecture docs | `application-service-hexagonal-architecture-documentation` | Hexagonal pattern |
| ArchUnit | `java-archunit` | Architecture enforcement |
| PostgreSQL | `datasource-postgresql` | Database config |
| JPA | `jpa-postgresql` | ORM setup |
| Flyway | `flyway`, `flyway-postgresql` | DB migrations (NOT Liquibase) |
| Kafka | `spring-boot-kafka` | Messaging |
| Gateway | `gateway` | Spring Cloud Gateway |
| OAuth2/Keycloak | `spring-boot-oauth2`, `spring-boot-oauth2-account` | IAM with Keycloak |
| OpenAPI | `springdoc-mvc-openapi`, `springdoc-oauth2` | API documentation |
| Code quality | `prettier` | Formatting |
| Logging test | `logs-spy` | Test log utilities |
