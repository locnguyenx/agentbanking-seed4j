---
name: seed4j-modules
description: Use when applying Seed4J modules and need to know the correct module slug for a technology
---

# Seed4J Module Mapping

## Overview

Seed4J module names (slugs) don't always match technology names. This skill maps technologies to actual module slugs discovered through `seed4j list`.

## Quick Reference: Technology → Module

| Technology | Seed4J Module Slug | Dependencies |
|------------|-------------------|--------------|
| Project init | `init` | - |
| Gradle (NOT Maven) | `gradle-java` | `init` |
| Gradle wrapper | `gradle-wrapper` | `gradle-java` |
| Java base classes | `java-base` | - |
| Spring Boot | `spring-boot` | - |
| Tomcat server | `spring-boot-tomcat` | `spring-boot` |
| Actuator | `spring-boot-actuator` | `spring-boot` |
| Hexagonal architecture docs | `application-service-hexagonal-architecture-documentation` | - |
| ArchUnit | `java-archunit` | - |
| PostgreSQL datasource | `datasource-postgresql` | `spring-boot` |
| JPA + PostgreSQL | `jpa-postgresql` | `spring-boot` |
| Flyway migrations | `flyway` | datasource |
| Flyway + PostgreSQL | `flyway-postgresql` | `flyway`, `datasource-postgresql` |
| Kafka | `spring-boot-kafka` | - |
| OAuth2 | `spring-boot-oauth2` | - |
| OAuth2 account | `spring-boot-oauth2-account` | `spring-boot-oauth2` |
| OpenAPI (MVC) | `springdoc-mvc-openapi` | - |
| OpenAPI + OAuth2 | `springdoc-oauth2` | `springdoc-mvc-openapi` |
| Spring Cloud Gateway | `gateway` | `spring-boot-webflux-empty`, `spring-cloud` |
| Spring Cloud | `spring-cloud` | - |
| Code formatting | `prettier` | - |
| Memoizers | `java-memoizers` | - |
| Enums | `java-enums` | - |
| Log spy (testing) | `logs-spy` | - |

## Common Mismatches

| You Might Search | Actual Module Slug |
|------------------|-------------------|
| kafka | `spring-boot-kafka` |
| gateway | `gateway` |
| keycloak | `spring-boot-oauth2` + `spring-boot-oauth2-account` |
| flyway | `flyway` + `flyway-postgresql` (for PostgreSQL) |
| liquibase | NOT AVAILABLE - use Flyway |
| maven | NOT RECOMMENDED - use `gradle-java` |
| axon | NOT AVAILABLE - manual setup required |

## Module Dependencies

Some modules require prerequisites:

```
init
 └─ gradle-java
     └─ gradle-wrapper

spring-boot
 ├─ datasource-postgresql
 │   └─ jpa-postgresql
 │       └─ flyway
 │           └─ flyway-postgresql
 ├─ spring-boot-tomcat
 └─ spring-boot-actuator

spring-boot-oauth2
 └─ spring-boot-oauth2-account

springdoc-mvc-openapi
 └─ springdoc-oauth2

spring-cloud
 └─ gateway (also needs spring-boot-webflux-empty)
```

## Complete Scaffolding Sequence

```bash
# Project setup
seed4j apply init --project-name "Project" --base-name Project --node-package-manager npm --no-commit
seed4j apply gradle-java --package-name com.example --no-commit
seed4j apply gradle-wrapper --no-commit

# Java + quality
seed4j apply java-base --no-commit
seed4j apply prettier --no-commit
seed4j apply java-memoizers --no-commit
seed4j apply java-enums --no-commit

# Spring Boot
seed4j apply spring-boot --no-commit
seed4j apply spring-boot-tomcat --server-port 8080 --no-commit
seed4j apply spring-boot-actuator --no-commit
seed4j apply application-service-hexagonal-architecture-documentation --no-commit
seed4j apply java-archunit --no-commit

# Database (Flyway, NOT Liquibase)
seed4j apply datasource-postgresql --no-commit
seed4j apply jpa-postgresql --no-commit
seed4j apply flyway --no-commit
seed4j apply flyway-postgresql --no-commit

# Messaging
seed4j apply spring-boot-kafka --no-commit

# Security (Keycloak via OAuth2)
seed4j apply spring-boot-oauth2 --no-commit
seed4j apply spring-boot-oauth2-account --no-commit

# API Documentation
seed4j apply springdoc-mvc-openapi --no-commit
seed4j apply springdoc-oauth2 --no-commit

# Gateway (if needed)
seed4j apply spring-cloud --no-commit
seed4j apply gateway --no-commit

# Testing
seed4j apply logs-spy --no-commit
```

## Technologies NOT in Seed4J

These require manual setup after scaffolding:

| Technology | Manual Setup Required |
|------------|----------------------|
| Axon Framework | Add dependencies to build.gradle.kts manually |
| Axon Server | Add to docker-compose.yml manually |
| Liquibase | Use Flyway instead |
| Testcontainers | Already included with some modules |

## Troubleshooting Generated Code

### ArchUnit Test Failures

**Symptom:** `HexagonalArchTest > Wire > should not have public classes` fails

**Cause:** Seed4J generates `KafkaProperties` as `public class` but ArchUnit expects `wire` package classes to be package-private.

**Fix:** Make class package-private:
```java
// Before (fails)
public class KafkaProperties { ... }

// After (passes)
class KafkaProperties { ... }
```

### Missing `logs-spy` Module

**Symptom:** `cannot find symbol: class Logs, LogsSpy, LogsSpyExtension`

**Cause:** `logs-spy` module not applied but test files reference it.

**Fix:** Apply the module:
```bash
seed4j apply logs-spy --no-commit
```

### Gateway Test Compilation Error

**Symptom:** `GatewayResourceIT.java` fails with missing `WebFluxTest`, `StepVerifier`

**Cause:** Integration test uses WebFlux test annotations but dependencies not in version catalog.

**Fix:** Delete the integration test file (it's sample code):
```bash
rm src/test/java/com/agentbanking/wire/gateway/infrastructure/primary/GatewayResourceIT.java
```

### Keycloak Port Conflict

**Symptom:** Port 8080 conflict with application server

**Cause:** Keycloak defaults to port 8080, same as Spring Boot.

**Fix:** Use port 9080 in docker-compose:
```yaml
keycloak:
  ports: ["9080:8080"]
```

Update application.yml OAuth2 issuer URI:
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: http://localhost:9080/realms/seed4j
```

## Post-Scaffolding Checklist

After scaffolding, verify:

1. **Build compiles:** `./gradlew build -x test`
2. **Tests pass:** `./gradlew test`
3. **ArchUnit passes:** Check wire package classes are package-private
4. **Ports configured:** Keycloak on 9080, app on 8080
5. **Flyway not Liquibase:** Check build.gradle.kts for correct dependencies
