# Agent Banking Platform - Full Implementation Plan

> **Status:** DRAFT - PENDING REVIEW
> **Version:** 3.0
> **Date:** 2026-04-13
> **Based on:** BRD v2.0, BDD v2.0, AGENTS.md (Spring Boot 4.0.1 + Spring Cloud 2025.1.0)

---

## Executive Summary

This plan outlines the complete implementation of the Agent Banking Platform based on:
- **BRD:** `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-brd.md`
- **BDD:** `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-bdd.md`

**Tech Stack (Confirmed):**
- Java 21 (LTS)
- Spring Boot 4.0.1
- Spring Cloud 2025.1.0 (Oakwood)
- Spring Cloud Gateway Server WebMVC 5.0.1
- Virtual Threads enabled (`spring.threads.virtual.enabled=true`)
- Temporal for Saga orchestration
- PostgreSQL (database-per-service)
- Redis for caching
- Kafka for async messaging

---

## Implementation Strategy

| Approach | Details |
|----------|----------|
| **Architecture** | Hexagonal (Ports & Adapters) via Seed4J |
| **Testing** | 3 Layers: Unit Tests → Integration Tests → BDD Scenarios |
| **External APIs** | Mocks for JPN, PayNet, CBS (realistic with delays/errors) |
| **Temporal** | Setup if missing, configure with retry policies |
| **Execution** | Sequential phases with parallel task groups |

---

## Phase 0: Infrastructure Check & Setup

**Duration:** 1 day

### 0.1 Check & Setup Temporal

| Task | Action | Verification |
|------|--------|--------------|
| Check existing Temporal config | Inspect docker-compose/k8s | N/A |
| If missing: Create docker-compose.yml | Include Temporal Server + PostgreSQL | `docker-compose up` works |
| Verify app-to-Temporal connection | Test client connectivity | Connection successful |

### 0.2 Verify Database Setup

| Task | Action | Verification |
|------|--------|--------------|
| Verify 6 PostgreSQL databases | rules_db, ledger_db, onboarding_db, switch_db, biller_db, commission_db | All accessible |
| Run existing Flyway migrations | Check migration status | All applied |
| Verify Redis connection | Ping Redis | PONG |
| Verify Kafka broker | Check broker connectivity | Broker available |

### 0.3 Verify Current Build

| Task | Action | Verification |
|------|--------|--------------|
| Run unit tests | `./gradlew test` | 602 tests pass |
| Run build | `./gradlew build -x integrationTest` | BUILD SUCCESSFUL |

---

## Phase 1: Foundation Services (Week 1)

**Duration:** 5 days  
**Parallel Groups:** Sequential (dependencies)

### 1.1 Transaction Core

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Complete TransactionEntity with all fields (ENT-3) | FR-2.4, FR-3.x | All transaction scenarios | Unit + Integration |
| Implement JournalEntryEntity (ENT-4) | FR-2.2 | BDD-L02 | Unit + Integration |
| Implement DoubleEntryService | FR-2.2 | BDD-L02 | Unit + Integration |
| Implement IdempotencyService (Redis) | FR-2.4 | BDD-L04-EC-02 | Unit + Integration |
| Implement AgentFloatRepository with PESSIMISTIC_WRITE | FR-2.3 | BDD-L03-EC-02 | Unit + Integration |
| Implement TransactionRepository | - | All scenarios | Unit + Integration |

### 1.2 Rules & Fee Engine

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Implement FeeConfig CRUD API | FR-1.1 | BDD-R01 | Unit + Integration |
| Implement VelocityRule CRUD API | FR-1.3 | BDD-R03 | Unit + Integration |
| Enhance FeeCalculationService | FR-1.4 | BDD-R04 | Unit + Integration |
| Implement daily limit check | FR-1.2 | BDD-R02 | Unit + Integration |
| Implement velocity check | FR-1.3 | BDD-R03 | Unit + Integration |

### 1.3 Ledger & Float Service

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Complete FloatService (debit/credit/lock) | FR-2.1, FR-2.3 | BDD-L03 | Unit + Integration |
| Implement LedgerService | FR-2.2 | BDD-L02 | Unit + Integration |
| Implement BalanceInquiryUseCase | FR-5.1, FR-5.2 | BDD-L01, BDD-L04 | Unit + Integration |
| Add PESSIMISTIC_WRITE locking | FR-2.3 | BDD-L03-EC-02 | Unit + Integration |
| Add geofencing validation | NFR-4.2 | BDD-W01-EC-05 | Unit + Integration |

**Phase 1 Deliverable:** Core transaction flow ready with tests

---

## Phase 2: Transaction Orchestration & Core Transactions (Week 2)

**Duration:** 5 days  
**Parallel Groups:** A, B (can run in parallel)

### Group A: Transaction Orchestrator

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Enhance CashWithdrawalWorkflow | FR-19.1, FR-19.2 | BDD-TO01 | Unit + Integration + BDD |
| Enhance CashDepositWorkflow | FR-19.1 | BDD-D01 | Unit + Integration + BDD |
| Implement compensation handlers | FR-19.4 | BDD-TO01-EC-02 | Unit + Integration |
| Add retry policies (ZERO for financial, exp backoff for non-financial) | FR-19.6, FR-18.4 | BDD-W01-EC-08 | Unit + Integration |
| Add Store & Forward for reversals | FR-18.1, FR-18.2 | BDD-TO01-EC-03 | Unit + Integration |

### Group B: Transaction Flows

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| SwitchAdapter (mock - ISO 8583) | FR-3.1 | BDD-W01 | Unit + Integration + BDD |
| PIN verification (mock) | FR-3.1 | BDD-W01-EC-01 | Unit + Integration |
| Reversal logic (MTI 0400) | FR-3.4, FR-18.1 | BDD-W01-EC-02, EC-03 | Unit + Integration + BDD |
| SMS notification | FR-3.1 | BDD-W01-SMS | Unit + Integration |
| Deposit workflow with ProxyEnquiry (mock) | FR-4.1, FR-2.5 | BDD-D01 | Unit + Integration + BDD |
| Account validation | FR-4.1 | BDD-D01 | Unit + Integration |
| Float cap check | FR-2.1 | BDD-D01-EC-04 | Unit + Integration |

**Phase 2 Deliverable:** Cash withdrawal/deposit via Temporal with compensation

---

## Phase 3: Onboarding & KYC (Week 3)

**Duration:** 4 days  
**Parallel Groups:** Sequential

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Implement KycVerificationEntity (ENT-7) | ENT-7 | BDD-O01 | Unit + Integration |
| JPN integration (mock - MyKad verification) | FR-6.1 | BDD-O01 | Unit + Integration + BDD |
| Biometric match (mock - thumbprint) | FR-6.2 | BDD-O02 | Unit + Integration + BDD |
| Auto-approval logic (match=YES + AML=CLEAN + age>=18) | FR-6.3 | BDD-O03 | Unit + Integration + BDD |
| Manual review queue logic | FR-6.4 | BDD-O03-EC-01, EC-02 | Unit + Integration |
| AgentRegistrationUseCase | US-O01 | BDD-O01 | Unit + Integration + BDD |

**Phase 3 Deliverable:** e-KYC with auto/manual approval flow

---

## Phase 4: API Gateway & Routes (Week 3-4)

**Duration:** 3 days  
**Parallel Groups:** Sequential

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Define gateway routes in YAML (spring.cloud.gateway.server.webmvc.routes) | FR-12.1 | All scenarios | Configuration + Integration |
| Configure JWT authentication | FR-12.2 | - | Integration |
| Add rate limiting | NFR-5.x | - | Integration |
| Document in OpenAPI 3.0 | FR-12.3 | - | Verification |

**Phase 4 Deliverable:** Gateway routes all POS requests with auth

---

## Phase 5: Bill Payments & Prepaid (Week 4-5)

**Duration:** 4 days  
**Parallel Groups:** Can run in parallel

### Bill Payments

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| BillerAdapterService (mock - JomPAY, ASTRO, TM, EPF) | FR-7.x | BDD-B01-B04 | Unit + Integration + BDD |
| JomPAY integration (mock) | FR-7.1 | BDD-B01 | Unit + Integration + BDD |
| Bill validation (Ref-1) | FR-7.5 | BDD-B01-EC-01 | Unit + Integration |
| Biller timeout handling | FR-7.1 | BDD-B01-EC-02 | Unit + Integration |

### Prepaid Top-up

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Top-up workflow (CELCOM, M1) | FR-8.x | BDD-T01-T02 | Unit + Integration + BDD |
| Phone validation | FR-8.3 | BDD-T01-EC-01 | Unit + Integration |
| Aggregator timeout handling | FR-8.1 | BDD-T01-EC-02 | Unit + Integration |

**Phase 5 Deliverable:** Bill payments and prepaid top-up working

---

## Phase 6: DuitNow & Merchant Services (Week 5-6)

**Duration:** 4 days  
**Parallel Groups:** Can run in parallel

### DuitNow Transfer

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| DuitNow workflow | FR-9.x | BDD-DNOW-01 | Unit + Integration + BDD |
| Proxy types (Mobile, NRIC, BRN) | FR-9.2 | BDD-DNOW-01-NRIC, BRN | Unit + Integration |
| ISO 20022 (mock) | FR-9.1 | BDD-DNOW-01 | Unit + Integration |
| 15-second timeout handling | FR-9.3 | BDD-DNOW-01-EC-02 | Unit + Integration |

### Merchant Services

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Retail sale workflow | FR-15.1 | BDD-M01 | Unit + Integration + BDD |
| PIN purchase workflow | FR-15.2 | BDD-M02 | Unit + Integration + BDD |
| MDR calculation | FR-15.4 | BDD-M01 | Unit + Integration |
| Cash-back flow | FR-15.5 | BDD-M03 | Unit + Integration |

**Phase 6 Deliverable:** DuitNow and merchant services working

---

## Phase 7: Settlement & Disputes (Week 6-7)

**Duration:** 4 days  
**Parallel Groups:** Sequential

### EOD Settlement

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Settlement calculation ((W+C+RS) - (D+BP)) | FR-16.1 | BDD-SM01 | Unit + Integration + BDD |
| Settlement file generation (CSV) | FR-16.3 | BDD-SM02 | Unit + Integration |
| Settlement direction (Bank owes Agent / Agent owes Bank) | FR-16.2 | BDD-SM01 | Unit + Integration |
| Exclude prefunding from daily net | FR-16.5 | BDD-SM01 | Unit + Integration |

### Discrepancy Resolution

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| Discrepancy detection (Ghost/Orphan/Mismatch) | FR-17.2 | BDD-DR01 | Unit + Integration + BDD |
| Maker workflow (propose adjustment) | FR-17.4 | BDD-DR01 | Unit + Integration + BDD |
| Checker workflow (approve/reject) | FR-17.5 | BDD-DR02 | Unit + Integration + BDD |
| Four-eyes principle (Maker != Checker) | FR-17.6 | BDD-DR03 | Unit + Integration |
| Pause settlement for disputed agents | FR-17.3 | BDD-SM04 | Unit + Integration |

**Phase 7 Deliverable:** Settlement and dispute resolution working

---

## Phase 8: Translation Layer - Tier 4 (Week 7-8)

**Duration:** 5 days  
**Parallel Groups:** Can run in parallel

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| ISO Translation Engine (JSON ↔ ISO 8583/20022) | FR-20.x | BDD-ISO01 | Unit + Integration + BDD |
| Persistent TCP connections to PayNet | FR-20.3 | BDD-ISO01 | Unit + Integration |
| STAN generation (000001-999999) | FR-20.4 | BDD-ISO01 | Unit + Integration |
| CBS Connector (mock - SOAP/MQ/Fixed-Length) | FR-21.x | BDD-CBS01 | Unit + Integration + BDD |
| HSM Wrapper (PIN block translation ZPK→LMK) | FR-22.x | BDD-HSM01 | Unit + Integration + BDD |
| Biller Gateway (JomPAY, Fiuu, TNB, Astro) | FR-23.x | BDD-BG01 | Unit + Integration + BDD |
| Error Mapping (ISO 8583, CBS, HSM → business exceptions) | FR-24.x | BDD-EM01 | Unit + Integration + BDD |

**Phase 8 Deliverable:** All Tier 4 adapters working with mocks

---

## Phase 9: Notification & Commission (Week 8)

**Duration:** 2 days  
**Parallel Groups:** Can run in parallel

| Task | BRD Ref | BDD Ref | Type | Testing |
|------|---------|---------|------|----------|
| NotificationService (Kafka producer) | - | BDD-W01-SMS | Unit + Integration |
| Commission calculation | - | - | Unit + Integration |
| Commission settlement | - | - | Unit + Integration |

**Phase 9 Deliverable:** Async notification and commission working

---

## Phase 10: Testing & Integration (Week 9-10)

**Duration:** 6 days  
**Parallel Groups:** Sequential

### 3-Layer Testing Implementation

| Layer | Coverage Target | Tools | Effort |
|-------|-----------------|-------|--------|
| **Unit Tests** | >80% coverage | JUnit 5, Mockito | Continuous |
| **Integration Tests** | All service interactions | Spring Boot Test, testcontainers | 3 days |
| **BDD Scenarios** | 30 key scenarios | Cucumber, JUnit 5 | 4 days |

### BDD Scenarios to Automate

| Priority | Scenario | BDD Ref | Type |
|----------|----------|--------|------|
| 1 | Cash Withdrawal Happy Path | BDD-TO01, BDD-W01 | Happy |
| 2 | Cash Deposit Happy Path | BDD-D01 | Happy |
| 3 | Fee Calculation | BDD-R01, BDD-R04 | Happy |
| 4 | Velocity Check Pass/Fail | BDD-R03, BDD-R03-EC-01 | Happy+Edge |
| 5 | KYC Auto-Approval | BDD-O03 | Happy |
| 6 | KYC Manual Review | BDD-O03-EC-01, EC-02 | Edge |
| 7 | Reversal Flow | BDD-W01-EC-02, EC-03 | Edge |
| 8 | Settlement Calculation | BDD-SM01 | Happy |
| 9 | Bill Payment | BDD-B01 | Happy |
| 10 | DuitNow Transfer | BDD-DNOW-01 | Happy |
| 11 | Discrepancy Resolution | BDD-DR01-DR03 | Happy+Edge |
| 12 | Insufficient Float | BDD-L03-EC-01 | Edge |
| 13 | Duplicate Idempotency | BDD-L04-EC-02 | Edge |

### Fix Integration Tests

| Task | Details |
|------|----------|
| Fix testcontainers config | Resolve schema validation issues |
| Fix Hibernate/JPA issues | Ensure proper entity mappings |
| Add test profiles | Different configs for test env |

**Phase 10 Deliverable:** Full test coverage with 3-layer strategy

---

## Summary Timeline

| Phase | Duration | Focus | Deliverable |
|-------|----------|-------|-------------|
| Phase 0 | 1 day | Infrastructure Check | Ready to start |
| Phase 1 | 5 days | Foundation Services | Transaction core ready |
| Phase 2 | 5 days | Orchestration | Cash withdrawal/deposit via Temporal |
| Phase 3 | 4 days | Onboarding | e-KYC flow |
| Phase 4 | 3 days | Gateway | Routes + JWT auth |
| Phase 5 | 4 days | Bill Payments | JomPAY, prepaid top-up |
| Phase 6 | 4 days | DuitNow + Merchant | DuitNow, retail, PIN purchase |
| Phase 7 | 4 days | Settlement | EOD settlement + disputes |
| Phase 8 | 5 days | Tier 4 Adapters | ISO, CBS, HSM, Biller |
| Phase 9 | 2 days | Notification + Commission | Async events |
| Phase 10 | 6 days | Testing | 3-layer test strategy |
| **Total** | **~43 days** | **Full Implementation** | Complete platform |

---

## Dependencies & Prerequisites

### Infrastructure Required

| Component | Status | Notes |
|----------|--------|-------|
| Temporal Server | TODO | Check & setup if missing |
| PostgreSQL (6 databases) | DONE | Already configured |
| Redis | DONE | Already configured |
| Kafka | TODO | Verify broker |

### External APIs (Mocks)

| API | Implementation | Notes |
|-----|----------------|-------|
| JPN (MyKad verification) | Mock | Realistic delays |
| PayNet (ISO 8583/20022) | Mock | TCP simulation |
| CBS | Mock | SOAP/MQ responses |
| HSM | Mock | PIN block operations |
| Billers (JomPAY, TNB, etc.) | Mock | Response validation |

---

## Key Decisions Made

| Decision | Rationale |
|----------|-----------|
| WebMVC Gateway (not WebFlux) | Spring Cloud 5.0.1 WebMVC is stable, Virtual Threads provide scalability |
| Virtual Threads enabled | WebFlux-level performance without reactive complexity |
| Mocks for external APIs | Faster development, no dependencies on external systems |
| 3-layer testing | Unit for logic, Integration for services, BDD for behavior |

---

## Next Steps

1. **Review this plan** - Confirm scope and priorities
2. **Start Phase 0** - Infrastructure check
3. **Begin Phase 1** - Foundation services

---

*Plan created based on BRD v2.0, BDD v2.0, and AGENTS.md specifications*