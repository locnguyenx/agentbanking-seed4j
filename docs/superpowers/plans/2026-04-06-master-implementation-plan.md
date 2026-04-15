# Agent Banking Platform — Master Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Agent Banking Platform MVP — a modular monolith with Temporal Saga orchestration for financial transactions.

**Architecture:** Modular Monolith using Seed4J hexagonal architecture. All bounded contexts live in one monorepo (`com.agentbanking.*`). Temporal Saga orchestrates multi-step transactions with compensation handlers. Database-per-service pattern with PostgreSQL. Kafka for async non-critical flows (SMS, commission).

**Tech Stack:** Java 25, Spring Boot 4, Spring Cloud, Spring Data JPA, PostgreSQL, Redis, Kafka, Temporal, Spring Cloud Gateway, JUnit 5, Mockito, ArchUnit

---

## Specs Reference

- **BRD v2.0:** `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-brd.md`
- **Design v2.0:** `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-design.md`
- **BDD v2.0:** `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-bdd.md`

---

## Project Status

The project has been scaffolded with Seed4J and has these existing bounded contexts (mostly stubs):

| Bounded Context | Package | Status |
|---|---|---|
| Rules | `com.agentbanking.rules` | Stub only |
| Ledger | `com.agentbanking.ledger` | Stub only |
| Commission | `com.agentbanking.commission` | Stub only |
| Onboarding | `com.agentbanking.onboarding` | Stub only |
| Biller | `com.agentbanking.biller` | Stub only |
| Switch Adapter | `com.agentbanking.switchadapter` | Stub only |

**Missing bounded contexts** (need to be created):
- Float (`com.agentbanking.float`)
- Transaction (`com.agentbanking.transaction`)
- Notification (`com.agentbanking.notification`)
- Settlement (`com.agentbanking.settlement`)
- Gateway (`com.agentbanking.gateway`)
- Orchestrator (`com.agentbanking.orchestrator`) — Temporal Saga engine
- ISO Adapter (`com.agentbanking.isoadapter`) — Tier 4
- CBS Adapter (`com.agentbanking.cbsadapter`) — Tier 4
- HSM Adapter (`com.agentbanking.hsmadapter`) — Tier 4

**Shared Kernels** (existing, working):
- `shared/error` — AssertionException framework
- `shared/authentication` — JWT reader/filter
- `shared/collection` — Collections utilities
- `shared/enumeration` — Enum utilities
- `shared/memoizer` — Memoization
- `shared/generation` — Code generation annotations

---

## Implementation Wave Overview

Implementation proceeds in **5 waves**:

### Wave 1: Foundation (Foundation Layer)
Infrastructure that all other contexts depend on.

| Plan | Bounded Context | Sub-plan File |
|---|---|---|
| 1.1 | Common Module — Error Code Registry & Global Error Schema | `plans/01-common-error-registry.md` |
| 1.2 | Transaction Domain Model — Money, Transaction, Agent, AgentFloat records | `plans/02-domain-model.md` |
| 1.3 | Temporal Saga Infrastructure — Temporal SDK setup, Workflow/Activity stubs | `plans/03-temporal-infrastructure.md` |
| 1.4 | Flyway Migrations — All database schemas | `plans/04-database-migrations.md` |

### Wave 2: Core Services (Business Logic)
Core services that handle business rules and state.

| Plan | Bounded Context | Sub-plan File |
|---|---|---|
| 2.1 | Rules & Fee Engine — Fee config, velocity rules, limit checks | `plans/05-rules-fee-engine.md` |
| 2.2 | Ledger & Float Service — Double-entry accounting, agent float | `plans/06-ledger-float.md` |
| 2.3 | Commission Service — Commission calculation and settlement | `plans/07-commission.md` |

### Wave 3: Transaction Orchestration (MVP Core)
The core MVP transaction flows coordinated by Temporal.

| Plan | Bounded Context | Sub-plan File |
|---|---|---|
| 3.1 | Transaction Orchestrator — Temporal Saga for Cash Withdrawal | `plans/08-transaction-orchestrator.md` |
| 3.2 | Cash Withdrawal Flow — Complete withdrawal saga with compensation | `plans/09-cash-withdrawal.md` |
| 3.3 | Cash Deposit Flow — Complete deposit saga with compensation | `plans/10-cash-deposit.md` |
| 3.4 | Idempotency & Redis Caching | `plans/11-idempotency-redis.md` |

### Wave 4: Supporting Services
Services that support the core transaction flows.

| Plan | Bounded Context | Sub-plan File |
|---|---|---|
| 4.1 | Onboarding Service — Agent registration, KYC | `plans/12-onboarding.md` |
| 4.2 | Notification Service — SMS via Kafka | `plans/13-notification.md` |
| 4.3 | Settlement Service — EOD processing | `plans/14-settlement.md` |

### Wave 5: Gateway & Tier 4 Adapters
External-facing gateway and translation layer.

| Plan | Bounded Context | Sub-plan File |
|---|---|---|
| 5.1 | API Gateway — Spring Cloud Gateway with JWT + rate limiting | `plans/15-api-gateway.md` |
| 5.2 | ISO Adapter (Tier 4) — ISO 8583 translation | `plans/16-iso-adapter.md` |
| 5.3 | CBS Adapter (Tier 4) — Core Banking connector | `plans/17-cbs-adapter.md` |
| 5.4 | HSM Adapter (Tier 4) — PIN block translation | `plans/18-hsm-adapter.md` |
| 5.5 | Biller Adapter (Tier 4) — Biller gateway | `plans/19-biller-adapter.md` |

---

## Wave 1: Foundation

### Wave 1.1: Common Module — Error Code Registry & Global Error Schema

**Sub-plan:** `docs/superpowers/plans/01-common-error-registry.md`

**BDD Scenarios:** Covers all error code scenarios (BDD-R01-EC-01, BDD-L01-EC-01, BDD-W01-EC-01, etc.)

**BRD Requirements:** FR-11 (Error Handling Design), US-EM01 (Tier 4 error normalization)

**Key Components:**
- `GlobalErrorSchema` record — standard error response format per Law III
- `ErrorCode` enum with all 60+ error codes organized by category
- `BusinessException` hierarchy (ValidationException, BusinessException, ExternalException, AuthException, SystemException)
- `GlobalExceptionHandler` — maps exceptions to HTTP status + GlobalErrorSchema
- `ErrorCodeMapper` — maps Tier 4 legacy codes to business exceptions

**Files to Create:**
- `src/main/java/com/agentbanking/shared/error/domain/GlobalErrorSchema.java`
- `src/main/java/com/agentbanking/shared/error/domain/ErrorCode.java`
- `src/main/java/com/agentbanking/shared/error/domain/BusinessException.java`
- `src/main/java/com/agentbanking/shared/error/domain/ValidationException.java`
- `src/main/java/com/agentbanking/shared/error/domain/BusinessRuleException.java`
- `src/main/java/com/agentbanking/shared/error/domain/ExternalServiceException.java`
- `src/main/java/com/agentbanking/shared/error/infrastructure/primary/GlobalExceptionHandler.java`
- `src/main/java/com/agentbanking/shared/error/infrastructure/primary/ErrorCodeMapper.java`
- `src/test/java/com/agentbanking/shared/error/domain/GlobalErrorSchemaTest.java`
- `src/test/java/com/agentbanking/shared/error/domain/ErrorCodeTest.java`
- `src/test/java/com/agentbanking/shared/error/infrastructure/primary/GlobalExceptionHandlerTest.java`

---

### Wave 1.2: Transaction Domain Model

**Sub-plan:** `docs/superpowers/plans/02-domain-model.md`

**BDD Scenarios:** All domain entities referenced across all BDD scenarios

**Key Components:**
- `Money` record — immutable monetary value with currency (MYR)
- `TransactionId`, `AgentId`, `AccountId`, `CustomerId` value objects
- `Transaction` record — entity with sagaExecutionId, status, sagaSteps
- `TransactionType` enum — CASH_WITHDRAWAL, CASH_DEPOSIT, BILL_PAYMENT, etc.
- `TransactionStatus` enum — PENDING, COMPLETED, FAILED, REVERSAL_INITIATED, REVERSED
- `Agent` record — agent entity with tier, status, bank account
- `AgentType` enum — MICRO, STANDARD, PREMIER
- `AgentStatus` enum — PENDING_APPROVAL, ACTIVE, DEACTIVATED
- `AgentFloat` record — virtual wallet with balance, reserved balance
- `FeeConfig` record — fee structure per transaction type and agent tier
- `VelocityRule` record — velocity limits per customer/MyKad

**Files to Create:**
- `src/main/java/com/agentbanking/transaction/domain/model/Money.java`
- `src/main/java/com/agentbanking/transaction/domain/model/TransactionId.java`
- `src/main/java/com/agentbanking/transaction/domain/model/TransactionType.java`
- `src/main/java/com/agentbanking/transaction/domain/model/TransactionStatus.java`
- `src/main/java/com/agentbanking/transaction/domain/model/Transaction.java`
- `src/main/java/com/agentbanking/onboarding/domain/model/AgentId.java`
- `src/main/java/com/agentbanking/onboarding/domain/model/AgentType.java`
- `src/main/java/com/agentbanking/onboarding/domain/model/AgentStatus.java`
- `src/main/java/com/agentbanking/onboarding/domain/model/Agent.java`
- `src/main/java/com/agentbanking/float/domain/model/AgentFloat.java`
- `src/main/java/com/agentbanking/rules/domain/model/FeeConfig.java`
- `src/main/java/com/agentbanking/rules/domain/model/VelocityRule.java`
- `src/main/java/com/agentbanking/shared/money/domain/Money.java` (if separate shared module)

---

### Wave 1.3: Temporal Saga Infrastructure

**Sub-plan:** `docs/superpowers/plans/03-temporal-infrastructure.md`

**BDD Scenarios:** BDD-TO01, BDD-TO01-EC-01, BDD-TO01-EC-02, BDD-TO01-EC-03

**Key Components:**
- Temporal SDK dependency setup in `build.gradle.kts`
- `TemporalProperties` configuration class
- `TemporalConfig` — Spring configuration for Temporal beans
- `SagaExecutionId` value object
- `SagaStepLog` record — records each step of a saga for audit
- `TransactionSagaState` — stores saga state (used by Workflow)
- Base `TransactionWorkflow` interface — all transaction workflows implement this
- Base `TransactionActivity` interface — all activities implement this
- `SagaStepLogRepository` port and implementation
- `SagaStepLogEntity` — JPA entity

**Files to Create:**
- `src/main/java/com/agentbanking/orchestrator/config/TemporalProperties.java`
- `src/main/java/com/agentbanking/orchestrator/config/TemporalConfig.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaExecutionId.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaStepLog.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaStepStatus.java`
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/SagaStepLogRepository.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/SagaStepLogEntity.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/SagaStepLogRepositoryImpl.java`

---

### Wave 1.4: Flyway Database Migrations

**Sub-plan:** `docs/superpowers/plans/04-database-migrations.md`

**Key Components:**
All Flyway migration files in `src/main/resources/db/migration/`:

| Migration | Contents |
|---|---|
| `V1__common_init.sql` | audit_log table |
| `V2__onboarding_init.sql` | agents, agent_users tables |
| `V3__float_init.sql` | agent_float, float_transaction tables |
| `V4__rules_init.sql` | fee_config, velocity_rule tables |
| `V5__transaction_init.sql` | transaction, idempotency_record, saga_step_log tables |
| `V6__ledger_init.sql` | account, ledger_entry tables |
| `V7__commission_init.sql` | commission_entry, commission_rate tables |
| `V8__notification_init.sql` | notification table |
| `V9__settlement_init.sql` | settlement_batch, reconciliation_record tables |

**Files to Create:**
- `src/main/resources/db/migration/V1__common_init.sql`
- `src/main/resources/db/migration/V2__onboarding_init.sql`
- `src/main/resources/db/migration/V3__float_init.sql`
- `src/main/resources/db/migration/V4__rules_init.sql`
- `src/main/resources/db/migration/V5__transaction_init.sql`
- `src/main/resources/db/migration/V6__ledger_init.sql`
- `src/main/resources/db/migration/V7__commission_init.sql`
- `src/main/resources/db/migration/V8__notification_init.sql`
- `src/main/resources/db/migration/V9__settlement_init.sql`

---

## Wave 2: Core Services

### Wave 2.1: Rules & Fee Engine

**Sub-plan:** `docs/superpowers/plans/05-rules-fee-engine.md`

**BDD Scenarios:** BDD-R01, BDD-R01-PCT, BDD-R01-EC-01 to BDD-R01-EC-03, BDD-R02, BDD-R02-EC-01 to BDD-R02-EC-04, BDD-R04, BDD-R03, BDD-R03-EC-01 to BDD-R03-EC-04

**BRD Requirements:** US-R01, US-R02, US-R03, US-R04, FR-1.1 to FR-1.4

**Key Components:**
- `FeeConfig` entity (already in Wave 1.2)
- `FeeConfigRepository` port + JPA implementation
- `VelocityRule` entity (already in Wave 1.2)
- `VelocityRuleRepository` port + JPA implementation
- `RulesService` — fee calculation, limit check, velocity check
- `FeeCalculationService` — split fee into customer/commission/bank share
- `ValidateTransactionUseCase` port
- `RulesController` REST endpoint

**Files to Create:**
- `src/main/java/com/agentbanking/rules/domain/port/out/FeeConfigRepository.java`
- `src/main/java/com/agentbanking/rules/domain/port/out/VelocityRuleRepository.java`
- `src/main/java/com/agentbanking/rules/domain/service/RulesService.java`
- `src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java`
- `src/main/java/com/agentbanking/rules/domain/port/in/ValidateTransactionUseCase.java`
- `src/main/java/com/agentbanking/rules/application/dto/FeeCalculationResult.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/FeeConfigEntity.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/FeeConfigRepositoryImpl.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/VelocityRuleEntity.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/VelocityRuleRepositoryImpl.java`
- `src/main/java/com/agentbanking/rules/infrastructure/primary/RulesController.java`
- `src/main/java/com/agentbanking/rules/config/RulesDatabaseConfig.java`
- `src/test/java/com/agentbanking/rules/domain/service/RulesServiceTest.java`
- `src/test/java/com/agentbanking/rules/domain/service/FeeCalculationServiceTest.java`

---

### Wave 2.2: Ledger & Float Service

**Sub-plan:** `docs/superpowers/plans/06-ledger-float.md`

**BDD Scenarios:** BDD-L01, BDD-L01-EC-01, BDD-L01-EC-02, BDD-L02, BDD-L03, BDD-L03-EC-01, BDD-L03-EC-02, BDD-L04, BDD-L04-EC-01, BDD-L04-EC-02, BDD-L02-DC, BDD-L02-MER, BDD-L02-PIN

**BRD Requirements:** US-L01, US-L02, US-L03, US-L04, FR-2.1 to FR-2.5

**Key Components:**
- `AgentFloat` entity with PESSIMISTIC_WRITE lock on balance updates
- `AgentFloatRepository` port + JPA implementation
- `LedgerService` — double-entry journal creation (debit + credit)
- `Account` entity, `LedgerEntry` entity
- `LedgerRepository` port + JPA implementation
- `FloatService` — debit/credit agent float
- `BalanceInquiryUseCase` port

**Files to Create:**
- `src/main/java/com/agentbanking/float/domain/port/out/AgentFloatRepository.java`
- `src/main/java/com/agentbanking/float/domain/service/FloatService.java`
- `src/main/java/com/agentbanking/float/domain/port/in/LockFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/DebitFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/CreditFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/BalanceInquiryUseCase.java`
- `src/main/java/com/agentbanking/float/infrastructure/persistence/entity/AgentFloatEntity.java`
- `src/main/java/com/agentbanking/float/infrastructure/persistence/entity/FloatTransactionEntity.java`
- `src/main/java/com/agentbanking/float/infrastructure/persistence/repository/AgentFloatRepositoryImpl.java`
- `src/main/java/com/agentbanking/float/infrastructure/primary/FloatController.java`
- `src/main/java/com/agentbanking/float/config/FloatDatabaseConfig.java`
- `src/main/java/com/agentbanking/ledger/domain/port/out/LedgerRepository.java`
- `src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`
- `src/main/java/com/agentbanking/ledger/domain/service/DoubleEntryService.java`
- `src/main/java/com/agentbanking/ledger/domain/model/AccountType.java`
- `src/main/java/com/agentbanking/ledger/domain/model/EntryType.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/AccountEntity.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/LedgerEntryEntity.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/LedgerRepositoryImpl.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/primary/LedgerController.java`
- `src/main/java/com/agentbanking/ledger/config/LedgerDatabaseConfig.java`
- `src/test/java/com/agentbanking/float/domain/service/FloatServiceTest.java`
- `src/test/java/com/agentbanking/ledger/domain/service/LedgerServiceTest.java`

---

### Wave 2.3: Commission Service

**Sub-plan:** `docs/superpowers/plans/07-commission.md`

**BDD Scenarios:** Commission calculation is referenced in BDD-W01, BDD-D01, BDD-B01, BDD-M01, etc.

**BRD Requirements:** FR-1.4 (fee splitting), FR-16 (commission settlement)

**Key Components:**
- `CommissionEntry` entity
- `CommissionRepository` port + JPA implementation
- `CommissionService` — calculate commission per transaction
- `SettleCommissionUseCase` — EOD commission settlement

**Files to Create:**
- `src/main/java/com/agentbanking/commission/domain/model/CommissionType.java`
- `src/main/java/com/agentbanking/commission/domain/model/CommissionStatus.java`
- `src/main/java/com/agentbanking/commission/domain/model/CommissionEntry.java`
- `src/main/java/com/agentbanking/commission/domain/port/out/CommissionRepository.java`
- `src/main/java/com/agentbanking/commission/domain/service/CommissionCalculationService.java`
- `src/main/java/com/agentbanking/commission/domain/service/CommissionSettlementService.java`
- `src/main/java/com/agentbanking/commission/domain/port/in/CalculateCommissionUseCase.java`
- `src/main/java/com/agentbanking/commission/domain/port/in/SettleCommissionUseCase.java`
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/entity/CommissionEntryEntity.java`
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/entity/CommissionRateEntity.java`
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/repository/CommissionRepositoryImpl.java`
- `src/main/java/com/agentbanking/commission/infrastructure/primary/CommissionController.java`
- `src/main/java/com/agentbanking/commission/config/CommissionDatabaseConfig.java`
- `src/test/java/com/agentbanking/commission/domain/service/CommissionCalculationServiceTest.java`

---

## Wave 3: Transaction Orchestration (MVP Core)

### Wave 3.1: Transaction Orchestrator

**Sub-plan:** `docs/superpowers/plans/08-transaction-orchestrator.md`

**BDD Scenarios:** BDD-TO01, BDD-TO01-EC-01, BDD-TO01-EC-02, BDD-TO01-EC-03

**BRD Requirements:** US-TO01, US-TO02, FR-19.1 to FR-19.7

**Key Components:**
- `TransactionOrchestrator` — coordinates all transaction flows
- `TransactionWorkflow` — Temporal Workflow interface
- `TransactionActivity` — Temporal Activity interface
- `IdempotencyService` — Redis-based idempotency with 24h TTL
- `TransactionSagaStateMachine` — manages saga state transitions
- `ReversalHandler` — handles Temporal compensation for reversals

**Files to Create:**
- `src/main/java/com/agentbanking/orchestrator/domain/model/TransactionSagaStateMachine.java`
- `src/main/java/com/agentbanking/orchestrator/domain/service/TransactionOrchestrator.java`
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/IdempotencyService.java`
- `src/main/java/com/agentbanking/orchestrator/domain/service/IdempotencyServiceImpl.java`
- `src/main/java/com/agentbanking/orchestrator/domain/service/ReversalHandler.java`
- `src/main/java/com/agentbanking/orchestrator/workflow/TransactionWorkflow.java`
- `src/main/java/com/agentbanking/orchestrator/activity/TransactionActivity.java`
- `src/main/java/com/agentbanking/orchestrator/workflow/impl/TransactionWorkflowImpl.java`
- `src/main/java/com/agentbanking/orchestrator/activity/impl/TransactionActivityImpl.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/TransactionEntity.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/IdempotencyRecordEntity.java`
- `src/main/java/com/agentbanking/orchestrator/config/OrchestratorDatabaseConfig.java`
- `src/test/java/com/agentbanking/orchestrator/domain/service/IdempotencyServiceTest.java`
- `src/test/java/com/agentbanking/orchestrator/workflow/TransactionWorkflowTest.java`

---

### Wave 3.2: Cash Withdrawal Flow

**Sub-plan:** `docs/superpowers/plans/09-cash-withdrawal.md`

**BDD Scenarios:** BDD-W01, BDD-W01-EC-01 to BDD-W01-EC-08, BDD-W01-SMS, BDD-W02, BDD-W02-EC-01

**BRD Requirements:** US-L05, US-L06, FR-3.1 to FR-3.5, US-V01, FR-18.1 to FR-18.5

**Key Components:**
- `CashWithdrawalWorkflow` — Temporal workflow for withdrawal
- `CashWithdrawalActivity` — activities: validate, lock float, send to switch, credit customer, debit float, calculate commission, notify
- `CashWithdrawalController` — REST endpoint for withdrawal
- `CashWithdrawalRequest` / `CashWithdrawalResponse` DTOs
- Compensation handlers for each step

**Files to Create:**
- `src/main/java/com/agentbanking/transaction/application/dto/CashWithdrawalRequest.java`
- `src/main/java/com/agentbanking/transaction/application/dto/CashWithdrawalResponse.java`
- `src/main/java/com/agentbanking/transaction/workflow/CashWithdrawalWorkflow.java`
- `src/main/java/com/agentbanking/transaction/workflow/impl/CashWithdrawalWorkflowImpl.java`
- `src/main/java/com/agentbanking/transaction/activity/CashWithdrawalActivity.java`
- `src/main/java/com/agentbanking/transaction/activity/impl/CashWithdrawalActivityImpl.java`
- `src/main/java/com/agentbanking/transaction/infrastructure/primary/TransactionController.java`
- `src/test/java/com/agentbanking/transaction/workflow/CashWithdrawalWorkflowTest.java`
- `src/test/java/com/agentbanking/transaction/activity/CashWithdrawalActivityTest.java`
- `src/test/java/com/agentbanking/transaction/infrastructure/primary/TransactionControllerTest.java`

---

### Wave 3.3: Cash Deposit Flow

**Sub-plan:** `docs/superpowers/plans/10-cash-deposit.md`

**BDD Scenarios:** BDD-D01, BDD-D01-EC-01 to BDD-D01-EC-04, BDD-D01-NIC, BDD-D01-BIO, BDD-D02, BDD-D02-EC-01

**BRD Requirements:** US-L07, US-L08, FR-4.1 to FR-4.3, FR-2.5

**Files to Create:**
- `src/main/java/com/agentbanking/transaction/application/dto/CashDepositRequest.java`
- `src/main/java/com/agentbanking/transaction/application/dto/CashDepositResponse.java`
- `src/main/java/com/agentbanking/transaction/workflow/CashDepositWorkflow.java`
- `src/main/java/com/agentbanking/transaction/workflow/impl/CashDepositWorkflowImpl.java`
- `src/main/java/com/agentbanking/transaction/activity/CashDepositActivity.java`
- `src/main/java/com/agentbanking/transaction/activity/impl/CashDepositActivityImpl.java`
- `src/test/java/com/agentbanking/transaction/workflow/CashDepositWorkflowTest.java`

---

### Wave 3.4: Idempotency & Redis Caching

**Sub-plan:** `docs/superpowers/plans/11-idempotency-redis.md`

**BDD Scenarios:** BDD-L04-EC-02, BDD-W01-EC-07 (duplicate with same idempotency key)

**BRD Requirements:** FR-2.4

**Key Components:**
- Redis configuration
- `IdempotencyService` with 24h TTL
- `TransactionCache` — caches transaction responses

---

## Wave 4: Supporting Services

### Wave 4.1: Onboarding Service

**Sub-plan:** `docs/superpowers/plans/12-onboarding.md`

**BDD Scenarios:** BDD-O01 to BDD-O05, BDD-A01, BDD-A01-EC-01 to BDD-A01-EC-03, BDD-A02, BDD-A02-EC-01, BDD-A02-EC-02

**BRD Requirements:** US-O01 to US-O05, US-A01, US-A02, FR-6.1 to FR-6.5, FR-14.3, FR-14.4

**Key Components:**
- `Agent` entity, `AgentUser` entity
- `AgentRepository` port + JPA implementation
- `OnboardingService` — agent registration, KYC verification
- `KycVerification` entity — JPN API integration
- `OnboardingController` — REST endpoints

---

### Wave 4.2: Notification Service

**Sub-plan:** `docs/superpowers/plans/13-notification.md`

**BDD Scenarios:** BDD-W01-SMS (SMS notification), BDD-D01-SMS

**BRD Requirements:** Notification per transaction

**Key Components:**
- `Notification` entity
- `NotificationRepository` port + JPA implementation
- `NotificationService` — dispatch SMS/Push via Kafka
- `NotificationProducer` — Kafka producer
- `NotificationConsumer` — Kafka consumer for async processing

---

### Wave 4.3: Settlement Service

**Sub-plan:** `docs/superpowers/plans/14-settlement.md`

**BDD Scenarios:** BDD-SM01, BDD-SM01-EC-01 to BDD-SM01-EC-03, BDD-SM01-MER, BDD-SM02, BDD-SM02-EC-01, BDD-SM03, BDD-SM03-EC-01 to BDD-SM03-EC-03, BDD-SM04, BDD-SM04-EC-01

**BRD Requirements:** US-SM01 to US-SM04, FR-16.1 to FR-17.3

**Key Components:**
- `SettlementBatch` entity, `ReconciliationRecord` entity
- `SettlementService` — EOD net settlement calculation
- `ReconciliationService` — Triple-Match against PayNet PSR
- `DiscrepancyCase` entity, `MakerCheckerService`

---

## Wave 5: Gateway & Tier 4 Adapters

### Wave 5.1: API Gateway

**Sub-plan:** `docs/superpowers/plans/15-api-gateway.md`

**BDD Scenarios:** BDD-G01, BDD-G02, BDD-G01-EC-01 to BDD-G01-EC-05

**BRD Requirements:** US-G01, US-G02, FR-12.1 to FR-12.3

**Key Components:**
- Spring Cloud Gateway configuration
- JWT authentication filter (reuse existing `shared/authentication`)
- Rate limiting filter
- Route configuration to backend services

---

### Wave 5.2: ISO Adapter (Tier 4)

**Sub-plan:** `docs/superpowers/plans/16-iso-adapter.md`

**BDD Scenarios:** BDD-ISO01, BDD-ISO01-EC-01, BDD-ISO01-EC-02, BDD-ISO02, BDD-ISO02-EC-01, BDD-ISO03, BDD-ISO03-EC-01

**BRD Requirements:** US-ISO01, FR-T4.1

**Key Components:**
- ISO 8583 message builder/parser
- TCP socket connection pool to PayNet
- ISO field mapping
- MTI 0100 (authorization), MTI 0110 (response), MTI 0400 (reversal)

---

### Wave 5.3: CBS Adapter (Tier 4)

**Sub-plan:** `docs/superpowers/plans/17-cbs-adapter.md`

**BDD Scenarios:** BDD-CBS01, BDD-CBS01-EC-01, BDD-CBS02, BDD-CBS02-EC-01

**BRD Requirements:** US-CBS01, FR-T4.2

**Key Components:**
- SOAP/MQ client for CBS
- `CbsAccount`, `CbsTransaction` models
- CBS connector with retry policy

---

### Wave 5.4: HSM Adapter (Tier 4)

**Sub-plan:** `docs/superpowers/plans/18-hsm-adapter.md`

**BDD Scenarios:** BDD-HSM01, BDD-HSM01-EC-01, BDD-HSM02, BDD-HSM02-EC-01

**BRD Requirements:** US-HSM01, FR-T4.3

**Key Components:**
- Thales HSM client
- PIN block generation (ISO 9564 Format 0)
- PIN verification
- Key vault integration

---

### Wave 5.5: Biller Adapter (Tier 4)

**Sub-plan:** `docs/superpowers/plans/19-biller-adapter.md`

**BDD Scenarios:** BDD-BG01, BDD-BG01-EC-01, BDD-BG01-EC-02, BDD-BG02, BDD-EM01, BDD-EM01-EC-01, BDD-EM02

**BRD Requirements:** US-BG01, US-EM01, FR-T4.4, FR-T4.5

**Key Components:**
- Multi-biller API normalization
- JomPAY, TNB, Astro, TM adapters
- Error code mapper (legacy → business)

---

## Implementation Notes

### Hexagonal Architecture Enforcement
Every bounded context MUST follow the hexagonal pattern. The existing `HexagonalArchTest` will enforce:
- Domain layer has ZERO framework imports
- Application layer depends only on domain
- Infrastructure.primary (adapters) doesn't depend on infrastructure.secondary
- Secondary adapters don't depend on application layer

### Law V Compliance
All domain services MUST be registered as Spring beans via `@Bean` in config classes, NOT via `@Service` annotation on domain classes.

### Law VII Compliance
Every `@FeignClient(url = "${property}")` MUST have matching entry in `application.yaml`.

### Database-per-service
Although this is a modular monolith, each bounded context has its own Flyway migration with a unique prefix (V1__, V2__, etc.) and separate schema initialization.

### Temporal Configuration
Temporal SDK requires:
- `io.temporal:temporal-spring-boot-starter` dependency
- `TemporalProperties` with namespace, host, port
- Workflow and Activity implementation beans
- Idempotent activity implementations (safe to retry)

---

## Summary

| Wave | Scope | Tasks | Approx. Lines of Code |
|---|---|---|---|
| 1 | Foundation | 4 sub-plans | ~2,000 |
| 2 | Core Services | 3 sub-plans | ~3,000 |
| 3 | Transaction Orchestration | 4 sub-plans | ~4,000 |
| 4 | Supporting Services | 3 sub-plans | ~2,500 |
| 5 | Gateway & Tier 4 | 5 sub-plans | ~3,500 |
| **Total** | **19 sub-plans** | **MVP + Tier 4** | **~15,000** |

**MVP Core (Waves 1-3):** Rules + Ledger + Float + Commission + Transaction Orchestrator + Cash Withdrawal + Cash Deposit + Idempotency
**MVP Completion (Wave 4):** Onboarding + Notification + Settlement
**Full Platform (Wave 5):** API Gateway + Tier 4 Adapters

---

## Next Steps

1. Create sub-plans for each bounded context (individual files in `docs/superpowers/plans/`)
2. Execute sub-plans in wave order using subagent-driven development
3. Run ArchUnit tests after each bounded context to verify hexagonal compliance
4. Run full build (`./gradlew build`) after each wave
5. Proceed to next wave only after all tests pass

---

*Plan created: 2026-04-06*
*Spec version: BRD v2.0, Design v2.0, BDD v2.0 (all dated 2026-04-06)*
