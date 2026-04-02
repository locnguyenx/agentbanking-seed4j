# Business Requirements Document (BRD)
## Seed4J Scaffolding Setup for Agent Banking Platform

**Version:** 1.0
**Date:** 2026-04-02
**Status:** Draft

---

## 1. Project Overview & Goals

### Project Name
Agent Banking Platform - Seed4J Scaffolding Setup

### Business Purpose
Establish the foundational project structure for the Agent Banking Platform using Seed4J CLI as the scaffolding tool, ensuring all microservices follow hexagonal architecture with consistent code quality and enforced architectural rules.

### Target Users
- Development team building the Agent Banking Platform
- Future developers maintaining the codebase

### Deliverables
1. **Seed4J CLI Setup** - Install and configure Seed4J CLI for project generation
2. **MVP Microservices Scaffolding** - 5 core services generated with hexagonal architecture:
   - Rules Service (fee engine, limits, velocity checks)
   - Ledger & Float Service (agent wallets, journal entries)
   - Onboarding Service (e-KYC, MyKad verification)
   - Switch Adapter Service (ISO 8583/20022 translation)
   - Biller Service (utility payments, biller webhooks)
3. **API Gateway** - Spring Cloud Gateway service
4. **Common Module** - Shared DTOs, error handling, utilities
5. **Monorepo Structure** - Gradle multi-project build with shared configurations
6. **Infrastructure Configuration** - Docker Compose for PostgreSQL, Redis, Kafka, Keycloak
7. **Architecture Enforcement** - ArchUnit tests for hexagonal architecture compliance

### Business Goals
1. Rapid project initialization with industry-standard architecture
2. Consistent code quality across all microservices
3. Enforced hexagonal architecture via ArchUnit tests
4. Ready-to-develop codebase with proper separation of concerns

---

## 2. User Stories

| ID | User Story | Priority |
|----|-----------|----------|
| US-S01 | As a developer, I want to install Seed4J CLI so I can generate services from the command line | High |
| US-S02 | As a developer, I want to generate each MVP service with hexagonal architecture so domain logic is isolated from infrastructure | High |
| US-S03 | As a developer, I want a Gradle monorepo structure so all services share common configurations | High |
| US-S04 | As a developer, I want a common module with GlobalError schema and shared DTOs | High |
| US-S05 | As a developer, I want Docker Compose for PostgreSQL, Redis, Kafka, Keycloak | Medium |
| US-S06 | As a developer, I want ArchUnit tests enforcing hexagonal architecture in every service | Medium |
| US-S07 | As a developer, I want SpringDoc OpenAPI configured for auto-generated API docs | Medium |

---

## 3. Functional Requirements

| ID | Requirement | US |
|----|------------|-----|
| FR-S01 | System shall provide Seed4J CLI installed and accessible via `seed4j` command | US-S01 |
| FR-S02 | Generated services shall follow hexagonal architecture (domain/application/infrastructure) | US-S02 |
| FR-S03 | Domain layer shall have ZERO imports from Spring, JPA, Kafka, or infrastructure | US-S02 |
| FR-S04 | Infrastructure layer shall implement ports defined in domain/port/ | US-S02 |
| FR-S05 | Monorepo shall use Gradle multi-project build with settings.gradle at root | US-S03 |
| FR-S06 | All services shall share common parent build configuration (Java 25, Spring Boot 4, dependencies) | US-S03 |
| FR-S07 | Common module shall provide: GlobalError schema, DTOs, validation utilities, error code registry | US-S04 |
| FR-S08 | Docker Compose shall include: PostgreSQL, Redis, Kafka, Keycloak | US-S05 |
| FR-S09 | Each service shall include ArchUnit tests verifying hexagonal architecture compliance | US-S06 |
| FR-S10 | Each service shall include SpringDoc OpenAPI configuration | US-S07 |
| FR-S11 | Seed4J CLI shall use `gradle-java` module for Gradle project setup (not Maven) | US-S03 |
| FR-S12 | Seed4J CLI shall use `spring-boot` module with Gradle variant | US-S02 |

---

## 4. MVP Services to Generate

| Service | Purpose | Key Ports |
|---------|---------|-----------|
| **Rules Service** | Fee engine, limits, velocity checks | FeeConfigPort, VelocityCheckPort |
| **Ledger & Float** | Agent wallets, journal entries, float management | FloatPort, JournalPort |
| **Onboarding** | e-KYC, MyKad verification, JPN integration | KycPort, JpnGatewayPort |
| **Switch Adapter** | ISO 8583/20022 translation, network management | SwitchPort, IsoTranslationPort |
| **Biller** | Utility payments, biller webhooks | BillerPort, BillerGatewayPort |
| **API Gateway** | Spring Cloud Gateway, JWT validation, routing | N/A (edge service) |
| **Common** | Shared DTOs, error handling, utilities | N/A (library) |

---

## 5. Non-Functional Requirements

| ID | Requirement |
|----|------------|
| NFR-S01 | Generated code shall have 100% test coverage for domain layer |
| NFR-S02 | All services shall start successfully with `./gradlew bootRun` |
| NFR-S03 | Docker Compose shall start all infrastructure services with single command |
| NFR-S04 | Build time for entire monorepo shall be under 5 minutes |

---

## 6. Constraints

| ID | Constraint |
|----|-----------|
| C-1 | Language: Java 25 (required by Seed4J) |
| C-2 | Framework: Spring Boot 4, Spring Cloud |
| C-3 | Build Tool: Gradle (NOT Maven) |
| C-4 | Database: PostgreSQL per microservice |
| C-5 | Architecture: Hexagonal (Ports & Adapters) per service |
| C-6 | Scaffolding Tool: Seed4J CLI (primary) or Seed4J REST API (fallback) |

---

## 7. Traceability Matrix: User Stories → Functional Requirements

| User Story | Functional Requirements | Priority |
|-----------|------------------------|----------|
| US-S01 | FR-S01 | High |
| US-S02 | FR-S02, FR-S03, FR-S04 | High |
| US-S03 | FR-S05, FR-S06, FR-S11, FR-S12 | High |
| US-S04 | FR-S07 | High |
| US-S05 | FR-S08 | Medium |
| US-S06 | FR-S09 | Medium |
| US-S07 | FR-S10 | Medium |
