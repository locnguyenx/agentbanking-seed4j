# Agent Banking Platform — Design Specification v2.0

**Version:** 2.0  
**Date:** 2026-04-06  
**Project:** Agent Banking Platform  
**Architecture:** Modular Monolith with Temporal Saga Orchestration

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture Pattern](#2-architecture-pattern)
3. [Project Structure](#3-project-structure)
4. [Domain Model](#4-domain-model)
5. [Transaction Orchestrator (Temporal Saga)](#5-transaction-orchestrator-temporal-saga)
6. [Tier 3: Domain Core Services](#6-tier-3-domain-core-services)
7. [Tier 4: Translation Layer Services](#7-tier-4-translation-layer-services)
8. [API Contracts](#8-api-contracts)
9. [Database Schema](#9-database-schema)
10. [Security Design](#10-security-design)
11. [Error Handling Design](#11-error-handling-design)
12. [Retry & Reversal Policy](#12-retry--reversal-policy)
13. [Monitoring & Observability](#13-monitoring--observability)
14. [Deployment Architecture](#14-deployment-architecture)

---

## 1. System Overview

### 1.1 Purpose

This document provides the technical design specification for the Agent Banking Platform, a financial services system enabling third-party retail locations (agents) to perform banking transactions on behalf of customers.

### 1.2 Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 4.x |
| Cloud | Spring Cloud | 2024.x |
| Persistence | Spring Data JPA (Hibernate) + PostgreSQL | Latest |
| Caching | Spring Data Redis | Latest |
| Messaging | Apache Kafka (Spring Cloud Stream) | Latest |
| Gateway | Spring Cloud Gateway (Reactive) | Latest |
| Saga Orchestration | Temporal | Latest |
| API Documentation | OpenAPI 3.0 | - |
| Testing | JUnit 5, Mockito, ArchUnit | Latest |
| Backoffice UI | React + TypeScript + Vite | Latest |
| IAM | OAuth2 + Keycloak | Latest |
| Monitoring | Micrometer + OpenTelemetry | Latest |

### 1.3 System Boundaries

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Agent Banking Platform                       │
│                                                                      │
│  ┌─────────────┐    ┌──────────────────────┐    ┌─────────────────┐ │
│  │   Channel   │    │  Spring Cloud        │    │   Tier 3        │ │
│  │   Layer      │───▶│  Gateway             │───▶│   Domain Core   │ │
│  │   (POS)      │    │  (JWT, Rate Limit)   │    │   Services      │ │
│  └─────────────┘    └──────────────────────┘    └────────┬────────┘ │
│                                                          │           │
│                                                          ▼           │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    Transaction Orchestrator                    │ │
│  │                    (Temporal Saga Engine)                      │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                            │                                        │
│            ┌───────────────┼───────────────┐                       │
│            ▼               ▼               ▼                       │
│  ┌─────────────────┐ ┌─────────────┐ ┌────────────────────┐       │
│  │ Tier 4          │ │ Tier 4      │ │ Tier 4             │       │
│  │ ISO Translation │ │ CBS         │ │ HSM                │       │
│  │ Engine          │ │ Connector   │ │ Wrapper            │       │
│  └────────┬────────┘ └──────┬──────┘ └─────────┬──────────┘       │
│           │                │                   │                  │
│           ▼                ▼                   ▼                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     Tier 5: Downstream Systems                │   │
│  │   (PayNet, JPN, Billers, Hardware Security Modules)         │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Architecture Pattern

### 2.1 Hexagonal Architecture (Ports & Adapters)

Every service MUST follow hexagonal architecture with strict layer separation:

```
service-name/
├── domain/                         # ZERO framework imports
│   ├── model/                      # Entities, Value Objects (Java Records)
│   │   ├── Agent.java              # Entity (no JPA annotations)
│   │   ├── Money.java              # Value Object
│   │   └── TransactionType.java    # Enum
│   ├── port/
│   │   ├── in/                     # Inbound ports (use cases)
│   │   │   ├── InitiateCashInUseCase.java
│   │   │   └── QueryFloatBalanceUseCase.java
│   │   └── out/                    # Outbound ports (repositories, gateways)
│   │       ├── AgentRepository.java
│   │       ├── LedgerPort.java
│   │       └── NotificationPort.java
│   └── service/                    # Business rules (pure Java)
│       ├── CashInService.java
│       └── FloatManagementService.java
├── application/                   # Use case orchestration, DTOs
│   ├── dto/
│   │   ├── CashInRequest.java
│   │   └── CashInResponse.java
│   └── service/
│       └── CashInApplicationService.java
├── infrastructure/                 # Adapters (implement ports)
│   ├── web/                        # REST controllers
│   │   └── CashInController.java
│   ├── persistence/                # JPA repositories
│   │   ├── repository/
│   │   │   └── JpaAgentRepository.java
│   │   └── entity/
│   │       └── AgentEntity.java
│   ├── messaging/                  # Kafka producers/consumers
│   └── external/                   # Feign clients
│       └── CbsFeignClient.java
└── config/                         # Spring configuration
    ├── DomainServiceConfig.java
    └── SecurityConfig.java
```

### 2.2 Architecture Laws (Enforced)

| Law | Rule | Enforcement |
|-----|------|-------------|
| Law I | DTOs only at controller boundaries | ArchUnit test |
| Law II | @Transactional on financial methods | ArchUnit test |
| Law III | Global Error Schema for all responses | Controller advice |
| Law IV | Database-per-service | Configuration |
| Law V | Domain services as Spring beans via @Bean | Config class |
| Law VI | Correct adapter annotations | ArchUnit test |
| Law VII | Feign URL from properties | ArchUnit test |
| Law VIII | compileOnly for service dependencies | build.gradle |
| Law IX | Component scan for common module | Main application |
| Law X | Pre-commit startup validation | CI pipeline |

---

## 3. Project Structure

### 3.1 Modular Monolith Structure

```
agentbanking/
├── common/                         # Shared code (no framework deps)
│   ├── model/                     # Shared value objects
│   ├── exception/                  # Common exceptions
│   ├── error/                      # Error code registry
│   └── util/                       # Utilities
├── gateway/                        # Spring Cloud Gateway
├── onboarding-service/             # Agent onboarding bounded context
├── float-service/                 # Float management bounded context
├── transaction-service/          # Transaction processing bounded context
├── ledger-service/               # Double-entry ledger bounded context
├── commission-service/           # Commission calculation bounded context
├── notification-service/          # SMS/Push notifications
├── settlement-service/           # EOD settlement processing
├── iso-adapter/                   # Tier 4: ISO 8583 translation
├── cbs-adapter/                   # Tier 4: CBS connector
├── hsm-adapter/                   # Tier 4: HSM wrapper
├── biller-adapter/                # Tier 4: Biller gateway
├── orchestrator/                   # Temporal Saga orchestrator
└── backoffice/                    # React frontend
```

### 3.2 Database Strategy

Each bounded context has its own PostgreSQL database:

| Service | Database | Purpose |
|---------|----------|---------|
| onboarding | onboarding_db | Agent/AgentUser entities |
| float | float_db | AgentFloat, FloatTransaction |
| transaction | transaction_db | Transaction, Idempotency |
| ledger | ledger_db | Double-entry accounts/entries |
| commission | commission_db | Commission calculations |
| notification | notification_db | Notification history |
| settlement | settlement_db | EOD settlement records |

---

## 4. Domain Model

### 4.1 Core Entities

```java
// domain/model/Agent.java (Entity - no JPA annotations)
package com.agentbanking.domain.model;

public record Agent(
    AgentId id,
    String businessRegistrationNumber,
    AgentName name,
    AgentType type,
    BankAccount bankAccount,
    Money currentFloatBalance,
    Money maxFloatLimit,
    AgentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public boolean canTransact(Money amount) {
        return status == AgentStatus.ACTIVE 
            && currentFloatBalance.isGreaterThanOrEqual(amount);
    }
}
```

```java
// domain/model/Money.java (Value Object)
package com.agentbanking.domain.model;

public record Money(
    BigDecimal amount,
    Currency currency  // ISO 4217 (MYR)
) {
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.of("MYR"));
    }
    
    public Money add(Money other) { ... }
    public Money subtract(Money other) { ... }
    public boolean isGreaterThanOrEqual(Money other) { ... }
}
```

```java
// domain/model/Transaction.java (Entity)
package com.agentbanking.domain.model;

public record Transaction(
    TransactionId id,
    TransactionType type,
    Money amount,
    AccountId sourceAccountId,
    AccountId destinationAccountId,
    AgentId agentId,
    String idempotencyKey,
    String sagaExecutionId,
    TransactionStatus status,
    List<SagaStep> sagaSteps,
    Instant initiatedAt,
    Instant completedAt
) { }
```

### 4.2 Value Objects

| Value Object | Purpose |
|--------------|---------|
| `Money` | Immutable monetary amount with currency |
| `AgentId` | Type-safe agent identifier |
| `TransactionId` | Type-safe transaction identifier |
| `AccountId` | Type-safe account identifier |
| `Currency` | ISO 4217 currency code |
| `PhoneNumber` | Validated phone number (E.164) |
| `MyKadNumber` | Encrypted MyKad identifier |

---

## 5. Transaction Orchestrator (Temporal Saga)

### 5.1 Temporal Integration Overview

The Transaction Orchestrator uses Temporal as the Saga orchestration engine. All financial operations flow through Temporal workflows.

**Key Benefits:**
-Durable execution: Transactions survive process crashes
- Built-in retry: Automatic retry with configurable policies
- Activity heartbeats: Long-running steps report progress
- Compensation handling: Saga pattern for rollback
- Distributed tracing: Built-in trace context propagation

### 5.2 Saga Workflow Definition

```java
// orchestrator/src/main/java/.../workflow/CashInWorkflow.java
package com.agentbanking.orchestrator.workflow;

@WorkflowInterface
public interface CashInWorkflow {
    
    @WorkflowMethod
    CashInResult execute(CashInWorkflowInput input);
    
    @Query
    String getStatus();
    
    @Signal
    void cancel();
}
```

### 5.3 Saga Workflow Implementation

```java
// orchestrator/src/main/java/.../workflow/impl/CashInWorkflowImpl.java
package com.agentbanking.orchestrator.workflow.impl;

public class CashInWorkflowImpl implements CashInWorkflow {
    
    private final CashInActivities activities;
    private String status = "INITIATED";
    
    @Override
    public CashInResult execute(CashInWorkflowInput input) {
        String sagaId = Workflow.getCurrentWorkflowId();
        
        try {
            // Step 1: Validate Transaction
            status = "VALIDATING";
            activities.validateTransaction(input);
            
            // Step 2: Lock Customer Account
            status = "LOCKING_CUSTOMER_ACCOUNT";
            String customerLockId = activities.lockCustomerAccount(
                input.customerAccountId(), 
                sagaId
            );
            
            // Step 3: Debit Agent Float (with compensation)
            status = "DEBITING_FLOAT";
            activities.debitAgentFloat(
                input.agentId(), 
                input.amount(),
                customerLockId  // compensation dependency
            );
            
            // Step 4: Credit Customer Account
            status = "CREDITING_CUSTOMER";
            activities.creditCustomerAccount(
                input.customerAccountId(),
                input.amount()
            );
            
            // Step 5: Calculate Commission
            status = "CALCULATING_COMMISSION";
            Money commission = activities.calculateCommission(
                input.amount(),
                TransactionType.CASH_IN
            );
            
            // Step 6: Send Notifications
            status = "SENDING_NOTIFICATIONS";
            activities.sendTransactionNotifications(input, commission);
            
            // Step 7: Complete
            status = "COMPLETED";
            return new CashInResult(
                TransactionId.generate(),
                TransactionStatus.COMPLETED,
                commission
            );
            
        } catch (ActivityFailure e) {
            status = "FAILED";
            return new CashInResult(
                null,
                TransactionStatus.FAILED,
                null
            );
        }
    }
    
    @Override
    public String getStatus() {
        return status;
    }
}
```

### 5.4 Saga Activities (Transactional Steps)

```java
// orchestrator/src/main/java/.../activity/CashInActivities.java
package com.agentbanking.orchestrator.activity;

@ActivityInterface
public interface CashInActivities {
    
    @ActivityMethod
    void validateTransaction(CashInWorkflowInput input);
    
    @ActivityMethod
    String lockCustomerAccount(AccountId accountId, String sagaId);
    
    @ActivityMethod
    void debitAgentFloat(AgentId agentId, Money amount, String lockDependency);
    
    @ActivityMethod
    void creditCustomerAccount(AccountId accountId, Money amount);
    
    @ActivityMethod
    Money calculateCommission(Money amount, TransactionType type);
    
    @ActivityMethod
    void sendTransactionNotifications(CashInWorkflowInput input, Money commission);
}
```

### 5.5 Activity Implementation with Compensation

```java
// orchestrator/src/main/java/.../activity/impl/CashInActivitiesImpl.java
package com.agentbanking.orchestrator.activity.impl;

public class CashInActivitiesImpl implements CashInActivities {
    
    private final AgentRepository agentRepository;
    private final CustomerRepository customerRepository;
    private final LedgerPort ledgerPort;
    private final NotificationPort notificationPort;
    
    @Override
    @ActivityMethod
    @Transactional
    public void debitAgentFloat(AgentId agentId, Money amount, String lockDependency) {
        // Acquire pessimistic lock
        AgentFloat floatAccount = agentRepository
            .findByAgentIdWithLock(agentId)
            .orElseThrow(() -> new AgentNotFoundException(agentId));
        
        // Verify sufficient balance
        if (!floatAccount.canDebit(amount)) {
            throw new InsufficientFloatException(amount, floatAccount.balance());
        }
        
        // Debit and record
        floatAccount.debit(amount);
        agentRepository.save(floatAccount);
        
        // Record ledger entry with compensation
        ledgerPort.recordEntry(LedgerEntry.debit(
            AccountType.AGENT_FLOAT,
            floatAccount.id(),
            amount,
            lockDependency  // Used for compensation
        ));
    }
    
    @Compensable
    public void compensateDebitAgentFloat(AgentId agentId, Money amount) {
        // This is called if a later step fails
        AgentFloat floatAccount = agentRepository.findByAgentId(agentId);
        floatAccount.credit(amount);
        agentRepository.save(floatAccount);
        
        ledgerPort.recordEntry(LedgerEntry.credit(
            AccountType.AGENT_FLOAT,
            floatAccount.id(),
            amount
        ));
    }
}
```

### 5.6 Saga Step Pattern

Each saga step follows this pattern:

```
┌──────────────────────────────────────────────────────────────┐
│                      SAG A STEP                               │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │   Execute        │───▶│   Compensation   │                │
│  │   (Do)           │    │   (Undo)         │                │
│  └─────────────────┘    └─────────────────┘                │
│         │                         ▲                          │
│         │                         │                          │
│         ▼                         │                          │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │   Activity      │    │   Compensating  │                 │
│  │   Implementation│───▶│   Activity      │                 │
│  └─────────────────┘    └─────────────────┘                 │
└──────────────────────────────────────────────────────────────┘
```

### 5.7 Idempotency with Temporal

```java
// Idempotency check at workflow start
@Override
public CashInResult execute(CashInWorkflowInput input) {
    // Check Redis for existing transaction
    String idempotencyKey = input.idempotencyKey();
    Transaction cachedResult = idempotencyCache.get(idempotencyKey);
    
    if (cachedResult != null) {
        // Return cached result - no reprocessing
        return new CashInResult(
            cachedResult.id(),
            cachedResult.status(),
            cachedResult.commission()
        );
    }
    
    // Continue with saga...
    
    // Cache result before returning
    idempotencyCache.put(
        idempotencyKey, 
        result, 
        Duration.ofHours(24)
    );
    
    return result;
}
```

### 5.8 Saga Timeout Configuration

```yaml
# application.yaml
temporal:
  activity:
    start-to-close-timeout: 5m
    schedule-to-start-timeout: 1m
    heartbeat-timeout: 30s
    retry-policy:
      initial-interval: 1s
      maximum-interval: 10s
      backoff-coefficient: 2.0
      maximum-attempts: 3
  
  workflow:
    execution-timeout: 30m
    run-timeout: 30m
```

---

## 6. Tier 3: Domain Core Services

### 6.1 Onboarding Service

**Bounded Context:** Agent Registration and User Management

```
onboarding-service/
├── domain/
│   ├── model/
│   │   ├── Agent.java
│   │   ├── AgentUser.java
│   │   ├── AgentStatus.java
│   │   └── UserRole.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── RegisterAgentUseCase.java
│   │   │   ├── ApproveAgentUseCase.java
│   │   │   └── CreateAgentUserUseCase.java
│   │   └── out/
│   │       ├── AgentRepository.java
│   │       ├── IamPort.java
│   │       └── DocumentVerificationPort.java
│   └── service/
│       ├── AgentRegistrationService.java
│       └── AgentApprovalService.java
├── application/
│   ├── dto/
│   │   ├── RegisterAgentRequest.java
│   │   ├── ApproveAgentRequest.java
│   │   └── CreateUserRequest.java
│   └── service/
│       └── OnboardingApplicationService.java
├── infrastructure/
│   ├── web/
│   │   └── OnboardingController.java
│   ├── persistence/
│   │   ├── repository/
│   │   │   └── JpaAgentRepository.java
│   │   └── entity/
│   │       └── AgentEntity.java
│   └── external/
│       ├── IamFeignClient.java
│       └── DocumentVerificationClient.java
└── config/
    └── DomainServiceConfig.java
```

**API Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/agents | Register new agent |
| GET | /api/v1/agents/{id} | Get agent details |
| POST | /api/v1/agents/{id}/approve | Approve agent |
| POST | /api/v1/agents/{id}/reject | Reject agent |
| POST | /api/v1/agents/{id}/users | Create agent user |
| PUT | /api/v1/agents/{id}/users/{userId} | Update user permissions |

### 6.2 Float Service

**Bounded Context:** Agent Float Management

```
float-service/
├── domain/
│   ├── model/
│   │   ├── AgentFloat.java
│   │   ├── FloatTransaction.java
│   │   ├── FloatTransactionType.java
│   │   └── FloatStatus.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── RequestFloatTopUpUseCase.java
│   │   │   ├── QueryFloatBalanceUseCase.java
│   │   │   └── ReconcileFloatUseCase.java
│   │   └── out/
│   │       ├── AgentFloatRepository.java
│   │       ├── CbsPort.java
│   │       └── LedgerPort.java
│   └── service/
│       ├── FloatTopUpService.java
│       └── FloatReconciliationService.java
├── application/
│   ├── dto/
│   │   ├── FloatTopUpRequest.java
│   │   └── FloatBalanceResponse.java
│   └── service/
│       └── FloatApplicationService.java
└── infrastructure/
    ├── web/
    │   └── FloatController.java
    ├── persistence/
    │   ├── repository/
    │   │   └── JpaAgentFloatRepository.java
    │   └── entity/
    │       └── AgentFloatEntity.java
    └── external/
        └── CbsFeignClient.java
```

**API Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/float/topup | Request float top-up |
| GET | /api/v1/float/{agentId}/balance | Get float balance |
| GET | /api/v1/float/{agentId}/transactions | Get transaction history |
| POST | /api/v1/float/{agentId}/reconcile | Submit reconciliation |

### 6.3 Transaction Service

**Bounded Context:** Transaction Processing and Orchestration

```
transaction-service/
├── domain/
│   ├── model/
│   │   ├── Transaction.java
│   │   ├── TransactionType.java
│   │   ├── TransactionStatus.java
│   │   └── IdempotencyRecord.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── InitiateCashInUseCase.java
│   │   │   ├── InitiateCashOutUseCase.java
│   │   │   ├── InitiateP2PTransferUseCase.java
│   │   │   ├── InitiateBillPaymentUseCase.java
│   │   │   └── ReverseTransactionUseCase.java
│   │   └── out/
│   │       ├── TransactionRepository.java
│   │       ├── SagaOrchestratorPort.java
│   │       └── NotificationPort.java
│   └── service/
│       └── TransactionValidationService.java
├── application/
│   ├── dto/
│   │   ├── CashInRequest.java
│   │   ├── CashOutRequest.java
│   │   ├── P2PTransferRequest.java
│   │   └── BillPaymentRequest.java
│   └── service/
│       └── TransactionApplicationService.java
└── infrastructure/
    ├── web/
    │   └── TransactionController.java
    ├── persistence/
    │   ├── repository/
    │   │   └── JpaTransactionRepository.java
    │   └── entity/
    │       └── TransactionEntity.java
    └── messaging/
        └── TransactionEventProducer.java
```

**API Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/transactions/cash-in | Initiate cash-in |
| POST | /api/v1/transactions/cash-out | Initiate cash-out |
| POST | /api/v1/transactions/p2p | Initiate P2P transfer |
| POST | /api/v1/transactions/bill-payment | Initiate bill payment |
| POST | /api/v1/transactions/{id}/reverse | Reverse transaction |
| GET | /api/v1/transactions/{id} | Get transaction details |
| GET | /api/v1/transactions/{id}/status | Get transaction status |

### 6.4 Ledger Service

**Bounded Context:** Double-Entry Accounting

```
ledger-service/
├── domain/
│   ├── model/
│   │   ├── Account.java
│   │   ├── LedgerEntry.java
│   │   ├── AccountType.java
│   │   └── EntryType.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── RecordEntryUseCase.java
│   │   │   └── QueryLedgerUseCase.java
│   │   └── out/
│   │       ├── LedgerRepository.java
│   │       └── AccountRepository.java
│   └── service/
│       ├── DoubleEntryService.java
│       └── BalanceCalculationService.java
└── infrastructure/
    ├── web/
    │   └── LedgerController.java
    ├── persistence/
    │   ├── repository/
    │   │   └── JpaLedgerRepository.java
    │   └── entity/
    │       └── LedgerEntryEntity.java
```

### 6.5 Commission Service

**Bounded Context:** Commission Calculation and Settlement

```
commission-service/
├── domain/
│   ├── model/
│   │   ├── CommissionEntry.java
│   │   ├── CommissionRate.java
│   │   └── CommissionType.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── CalculateCommissionUseCase.java
    │   │   └── SettleCommissionUseCase.java
│   │   └── out/
│   │       ├── CommissionRepository.java
│   │       └── SettlementPort.java
│   └── service/
│       ├── CommissionCalculationService.java
│       └── CommissionSettlementService.java
└── infrastructure/
    ├── web/
    │   └── CommissionController.java
    ├── persistence/
    │   ├── repository/
    │   │   └── JpaCommissionRepository.java
    │   └── entity/
    │       └── CommissionEntryEntity.java
```

### 6.6 Notification Service

**Bounded Context:** Customer and Agent Notifications

```
notification-service/
├── domain/
│   ├── model/
│   │   ├── Notification.java
│   │   ├── NotificationType.java
│   │   └── NotificationStatus.java
│   ├── port/
│   │   ├── in/
    │   │   └── SendNotificationUseCase.java
│   │   └── out/
│   │       ├── SmsGatewayPort.java
│   │       ├── PushNotificationPort.java
│   │       └── EmailPort.java
│   └── service/
│       └── NotificationDispatchService.java
└── infrastructure/
    ├── web/
    │   └── NotificationController.java
    ├── messaging/
    │   └── NotificationConsumer.java
    └── external/
        └── SmsGatewayClient.java
```

### 6.7 Settlement Service

**Bounded Context:** EOD Processing and Reconciliation

```
settlement-service/
├── domain/
│   ├── model/
│   │   ├── SettlementBatch.java
│   │   ├── ReconciliationRecord.java
│   │   └── SettlementStatus.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── ProcessEodSettlementUseCase.java
│   │   │   └── ReconcileFloatUseCase.java
│   │   └── out/
│   │       ├── TransactionRepository.java
│   │       ├── CommissionRepository.java
│   │       └── ReportGeneratorPort.java
│   └── service/
│       ├── EodProcessingService.java
│       └── ReconciliationService.java
└── infrastructure/
    ├── web/
    │   └── SettlementController.java
    └── persistence/
        └── repository/
            └── JpaSettlementRepository.java
```

---

## 7. Tier 4: Translation Layer Services

### 7.1 ISO Adapter (ISO 8583 Translation Engine)

Converts internal transaction format to/from ISO 8583 messages for payment network communication.

```
iso-adapter/
├── domain/
│   ├── model/
│   │   ├── IsoMessage.java
│   │   ├── IsoField.java
│   │   └── IsoMessageType.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── TranslateToIsoUseCase.java
│   │   │   └── TranslateFromIsoUseCase.java
│   │   └── out/
│   │       └── SwitchPort.java
│   └── service/
│       └── IsoTranslationService.java
├── application/
│   ├── dto/
│   │   ├── InternalTransaction.java
│   │   └── IsoTransactionResponse.java
│   └── service/
│       └── IsoTranslationApplicationService.java
└── infrastructure/
    ├── web/
    │   └── IsoController.java
    ├── external/
    │   └── SwitchFeignClient.java
    └── codec/
        └── IsoMessageCodec.java
```

**Supported ISO 8583 Message Types:**

| MTI | Description |
|-----|-------------|
| 0100 | Financial Transaction Request |
| 0110 | Financial Transaction Response |
| 0120 | Reversal Request |
| 0121 | Reversal Response |
| 0200 | Network Management Request |
| 0210 | Network Management Response |

### 7.2 CBS Adapter (Core Banking System Connector)

Connects to the bank's Core Banking System for account management and fund transfers.

```
cbs-adapter/
├── domain/
│   ├── model/
│   │   ├── CbsAccount.java
│   │   ├── CbsTransaction.java
│   │   └── CbsResponse.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateAccountUseCase.java
│   │   │   ├── TransferFundsUseCase.java
│   │   │   └── InquiryAccountUseCase.java
│   │   └── out/
│   │       └── CbsGatewayPort.java
│   └── service/
│       └── CbsIntegrationService.java
├── application/
│   ├── dto/
│   │   ├── CbsAccountRequest.java
│   │   └── CbsTransferRequest.java
│   └── service/
│       └── CbsApplicationService.java
└── infrastructure/
    ├── web/
    │   └── CbsController.java
    └── external/
        └── CbsFeignClient.java
```

**CBS Operations:**

| Operation | Description |
|-----------|-------------|
| CREATE_ACCOUNT | Create new customer account |
| GET_ACCOUNT | Retrieve account details |
| DEBIT_ACCOUNT | Debit funds from account |
| CREDIT_ACCOUNT | Credit funds to account |
| TRANSFER | Internal fund transfer |
| GET_BALANCE | Query account balance |

### 7.3 HSM Adapter (Hardware Security Module Wrapper)

Provides secure PIN management and cryptographic operations.

```
hsm-adapter/
├── domain/
│   ├── model/
│   │   ├── PinBlock.java
│   │   ├── EncryptedData.java
│   │   └── KeyScheme.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── EncryptPinUseCase.java
│   │   │   ├── VerifyPinUseCase.java
│   │   │   └── GeneratePinUseCase.java
│   │   └── out/
│   │       └── HsmGatewayPort.java
│   └── service/
│       ├── PinManagementService.java
│       └── EncryptionService.java
├── application/
│   ├── dto/
│   │   ├── PinVerificationRequest.java
│   │   └── PinGenerationRequest.java
│   └── service/
│       └── HsmApplicationService.java
└── infrastructure/
    ├── web/
    │   └── HsmController.java
    └── external/
        └── HsmFeignClient.java
```

**HSM Operations:**

| Operation | Description |
|-----------|-------------|
| GENERATE_PIN | Generate random 6-digit PIN |
| ENCRYPT_PIN | Encrypt PIN to PIN block |
| VERIFY_PIN | Verify PIN against encrypted block |
| DECRYPT_PIN | Decrypt PIN block (HSM only) |
| GENERATE_KEY | Generate cryptographic key |
| ENCRYPT_DATA | Encrypt data with key |
| DECRYPT_DATA | Decrypt data with key |

### 7.4 Biller Adapter (Biller Gateway)

Connects to billers (utilities, telco, insurance) for bill payment processing.

```
biller-adapter/
├── domain/
│   ├── model/
│   │   ├── Biller.java
│   │   ├── BillerTransaction.java
│   │   └── BillerStatus.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── GetBillUseCase.java
│   │   │   ├── PayBillUseCase.java
│   │   │   └── InquiryBillerUseCase.java
│   │   └── out/
│   │       ├── BillerGatewayPort.java
│   │       └── BillerRegistryPort.java
│   └── service/
│       ├── BillPaymentService.java
│       └── BillerInquiryService.java
├── application/
│   ├── dto/
│   │   ├── BillInquiryRequest.java
│   │   ├── BillPaymentRequest.java
│   │   └── BillPaymentResponse.java
│   └── service/
│       └── BillerApplicationService.java
└── infrastructure/
    ├── web/
    │   └── BillerController.java
    ├── external/
    │   └── BillerFeignClient.java
    └── registry/
        └── BillerRegistry.java
```

**Supported Billers:**

| Biller Code | Biller Name | Category |
|-------------|-------------|----------|
| TNB | Tenaga Nasional Berhad | Utility |
| SAADAQ | Sabah Electricity | Utility |
| AIR | Air Selangor | Utility |
| MAXIS | Maxis Berhad | Telco |
| DIGI | Digi Telecommunications | Telco |
| UNIFI | Telekom Malaysia | Telco |

---

## 8. API Contracts

### 8.1 Internal OpenAPI Specification

Each service exposes an internal OpenAPI spec at `/docs/openapi-internal.yaml`.

**Common API Response Format:**

```yaml
success:
  status: SUCCESS
  data:
    transactionId: "TXN-20260406-001"
    status: COMPLETED
    amount: 500.00
    currency: MYR
    agentId: "AGT-20260406-001"
    customerId: "CUST-123456"
    timestamp: "2026-04-06T10:30:00+08:00"
    commission: 2.50

error:
  status: FAILED
  error:
    code: "ERR_BIZ_201"
    message: "Insufficient float balance for transaction"
    action_code: "DECLINE"
    trace_id: "trace-abc123"
    timestamp: "2026-04-06T10:30:00+08:00"
```

### 8.2 API Endpoint Summary

| Service | Domain | Endpoints |
|---------|--------|-----------|
| gateway | /api/v1/* | Routes to services |
| onboarding | /api/v1/agents/* | Agent management |
| float | /api/v1/float/* | Float operations |
| transaction | /api/v1/transactions/* | Transaction processing |
| ledger | /api/v1/ledger/* | Accounting |
| commission | /api/v1/commission/* | Commission |
| notification | /api/v1/notifications/* | Notifications |
| settlement | /api/v1/settlement/* | EOD processing |
| iso-adapter | /api/v1/iso/* | ISO translation |
| cbs-adapter | /api/v1/cbs/* | CBS operations |
| hsm-adapter | /api/v1/hsm/* | Security ops |
| biller-adapter | /api/v1/billers/* | Bill payment |

---

## 9. Database Schema

### 9.1 Common Tables

```sql
-- Flyway migration: V1__common_init.sql

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100),
    actor_type VARCHAR(50),
    payload JSONB,
    trace_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_trace ON audit_log(trace_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);
```

### 9.2 Onboarding Tables

```sql
-- Flyway migration: V1__onboarding_init.sql

CREATE TABLE agent (
    id VARCHAR(50) PRIMARY KEY,
    business_registration_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    bank_code VARCHAR(10) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    primary_contact VARCHAR(20) NOT NULL,
    business_address TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    max_float_limit DECIMAL(19, 4) NOT NULL DEFAULT 10000.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_user (
    id VARCHAR(50) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL REFERENCES agent(id),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE user_permission (
    user_id VARCHAR(50) NOT NULL REFERENCES agent_user(id),
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission)
);

CREATE INDEX idx_agent_status ON agent(status);
CREATE INDEX idx_agent_brn ON agent(business_registration_number);
CREATE INDEX idx_agent_user_agent ON agent_user(agent_id);
```

### 9.3 Float Tables

```sql
-- Flyway migration: V1__float_init.sql

CREATE TABLE agent_float (
    id VARCHAR(50) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE REFERENCES agent(id),
    current_balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    pending_balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE float_transaction (
    id VARCHAR(50) PRIMARY KEY,
    float_id VARCHAR(50) NOT NULL REFERENCES agent_float(id),
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    balance_before DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    reference_id VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_float_agent ON agent_float(agent_id);
CREATE INDEX idx_float_txn_float ON float_transaction(float_id);
CREATE INDEX idx_float_txn_created ON float_transaction(created_at);
```

### 9.4 Transaction Tables

```sql
-- Flyway migration: V1__transaction_init.sql

CREATE TABLE transaction (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    source_account_id VARCHAR(50),
    destination_account_id VARCHAR(50),
    agent_id VARCHAR(50) NOT NULL REFERENCES agent(id),
    idempotency_key VARCHAR(100) UNIQUE,
    saga_execution_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    reversal_of VARCHAR(50) REFERENCES transaction(id),
    error_code VARCHAR(50),
    error_message TEXT,
    metadata JSONB,
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE saga_step_log (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL REFERENCES transaction(id),
    step_name VARCHAR(100) NOT NULL,
    step_order INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    compensation_executed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_txn_agent ON transaction(agent_id);
CREATE INDEX idx_txn_status ON transaction(status);
CREATE INDEX idx_txn_idempotency ON transaction(idempotency_key);
CREATE INDEX idx_txn_saga ON transaction(saga_execution_id);
CREATE INDEX idx_txn_initiated ON transaction(initiated_at);
CREATE INDEX idx_saga_txn ON saga_step_log(transaction_id);
```

### 9.5 Ledger Tables

```sql
-- Flyway migration: V1__ledger_init.sql

CREATE TABLE account (
    id VARCHAR(50) PRIMARY KEY,
    account_type VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50),
    owner_type VARCHAR(50),
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL REFERENCES account(id),
    entry_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    balance_before DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    transaction_id VARCHAR(50),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_type ON account(account_type);
CREATE INDEX idx_account_owner ON account(owner_id, owner_type);
CREATE INDEX idx_entry_account ON ledger_entry(account_id);
CREATE INDEX idx_entry_txn ON ledger_entry(transaction_id);
CREATE INDEX idx_entry_created ON ledger_entry(created_at);
```

### 9.6 Commission Tables

```sql
-- Flyway migration: V1__commission_init.sql

CREATE TABLE commission_entry (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    commission_type VARCHAR(50) NOT NULL,
    transaction_amount DECIMAL(19, 4) NOT NULL,
    commission_amount DECIMAL(19, 4) NOT NULL,
    rate_applied DECIMAL(10, 6) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE commission_rate (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50),
    agent_tier VARCHAR(50),
    transaction_type VARCHAR(50) NOT NULL,
    rate_type VARCHAR(50) NOT NULL,
    rate_value DECIMAL(10, 6) NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_commission_agent ON commission_entry(agent_id);
CREATE INDEX idx_commission_txn ON commission_entry(transaction_id);
CREATE INDEX idx_commission_status ON commission_entry(status);
CREATE INDEX idx_commission_rate_type ON commission_rate(transaction_type);
```

---

## 10. Security Design

### 10.1 Authentication Flow

```
┌─────────┐      ┌─────────────┐      ┌─────────┐      ┌──────────┐
│  Agent  │─────▶│   Gateway   │─────▶│   IAM   │─────▶│ Keycloak │
│   App   │      │   (JWT)     │      │ Service │      │          │
└─────────┘      └─────────────┘      └─────────┘      └──────────┘
                        │                                      │
                        │         ┌─────────────┐             │
                        └────────▶│   Backend   │◀────────────┘
                                  │   Services  │
                                  └─────────────┘
```

**JWT Token Structure:**
```json
{
  "sub": "USR-20260406-001",
  "agent_id": "AGT-20260406-001",
  "username": "kedai_abc_admin",
  "roles": ["AGENT_ADMIN"],
  "permissions": ["CASH_IN", "CASH_OUT", "BALANCE_INQUIRY"],
  "iat": 1712380200,
  "exp": 1712383800,
  "iss": "agentbanking-iam"
}
```

### 10.2 Authorization Matrix

| Role | CASH_IN | CASH_OUT | P2P | BILL_PAY | FLOAT_TOPUP | REVERSAL |
|------|---------|----------|-----|----------|-------------|----------|
| AGENT_ADMIN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| AGENT_TELLER | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| AGENT_VIEWER | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| BANK_OPS | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| COMPLIANCE | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

### 10.3 PIN Security

- PINs are NEVER stored in plaintext
- PINs are encrypted by HSM using Thales HSM
- PIN verification happens entirely within HSM boundary
- PIN blocks use ISO 9564 format 0 (ISO-1)

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client     │─────▶│   HSM       │─────▶│   Result    │
│  (PIN Block) │      │  (Verify)   │      │  (Match?)   │
└─────────────┘      └─────────────┘      └─────────────┘
```

### 10.4 Data Protection

| Data Type | At Rest | In Transit | In Logs |
|-----------|---------|------------|---------|
| Card Numbers | Encrypted (AES-256) | TLS 1.3 | Masked |
| MyKad Numbers | Encrypted (AES-256) | TLS 1.3 | NEVER |
| PIN Blocks | HSM Encrypted | TLS 1.3 | NEVER |
| Account Numbers | Encrypted (AES-256) | TLS 1.3 | Masked |
| Phone Numbers | Encrypted | TLS 1.3 | Masked |
| Transaction Amounts | Encrypted | TLS 1.3 | Allowed |

---

## 11. Error Handling Design

### 11.1 Error Code Registry

| Code Range | Category | Description |
|------------|----------|-------------|
| ERR_VAL_xxx | Validation | Input validation errors |
| ERR_BIZ_xxx | Business | Business rule violations |
| ERR_EXT_xxx | External | Downstream service errors |
| ERR_AUTH_xxx | Authentication | Auth/authz failures |
| ERR_SYS_xxx | System | Internal system errors |

### 11.2 Error Mapping Table

| Internal Error | External Code | HTTP Status | Action |
|----------------|---------------|-------------|--------|
| INVALID_REQUEST | ERR_VAL_001 | 400 | DECLINE |
| INVALID_AMOUNT | ERR_VAL_002 | 400 | DECLINE |
| INVALID_ACCOUNT | ERR_VAL_003 | 400 | DECLINE |
| INSUFFICIENT_FLOAT | ERR_BIZ_201 | 400 | DECLINE |
| INSUFFICIENT_BALANCE | ERR_BIZ_202 | 400 | DECLINE |
| DAILY_LIMIT_EXCEEDED | ERR_BIZ_203 | 400 | DECLINE |
| REVERSAL_WINDOW_EXPIRED | ERR_BIZ_301 | 400 | DECLINE |
| ALREADY_REVERSED | ERR_BIZ_302 | 400 | DECLINE |
| CBS_TIMEOUT | ERR_EXT_101 | 504 | RETRY |
| CBS_ERROR | ERR_EXT_102 | 502 | RETRY |
| BILLER_TIMEOUT | ERR_EXT_201 | 504 | RETRY |
| BILLER_ERROR | ERR_EXT_202 | 502 | RETRY |
| INVALID_CREDENTIALS | ERR_AUTH_001 | 401 | DECLINE |
| ACCOUNT_LOCKED | ERR_AUTH_002 | 403 | REVIEW |
| SANCTIONS_BLOCK | ERR_AUTH_101 | 403 | REVIEW |
| INTERNAL_ERROR | ERR_SYS_001 | 500 | REVIEW |

### 11.3 Global Error Schema

```java
// common/src/main/java/.../error/GlobalErrorSchema.java
package com.agentbanking.common.error;

public record GlobalErrorSchema(
    String status,
    ErrorDetail error
) {
    public record ErrorDetail(
        String code,
        String message,
        String actionCode,
        String traceId,
        Instant timestamp
    ) {}
    
    public static GlobalErrorSchema of(String code, String message, String actionCode) {
        return new GlobalErrorSchema(
            "FAILED",
            new ErrorDetail(
                code,
                message,
                actionCode,
                TraceId.getCurrent(),
                Instant.now()
            )
        );
    }
}
```

### 11.4 Controller Advice

```java
// common/src/main/java/.../error/GlobalExceptionHandler.java
package com.agentbanking.common.error;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalErrorSchema> handleBusinessException(
        BusinessException ex,
        WebRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(GlobalErrorSchema.of(
                ex.getErrorCode(),
                ex.getMessage(),
                "DECLINE"
            ));
    }
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<GlobalErrorSchema> handleExternalException(
        ExternalServiceException ex,
        WebRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(GlobalErrorSchema.of(
                ex.getErrorCode(),
                ex.getMessage(),
                "RETRY"
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalErrorSchema> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(GlobalErrorSchema.of(
                "ERR_SYS_001",
                "An internal error occurred",
                "REVIEW"
            ));
    }
}
```

---

## 12. Retry & Reversal Policy

### 12.1 Retry Policy by Error Type

| Error Type | Retry | Max Attempts | Backoff |
|------------|-------|--------------|---------|
| Network timeout | Yes | 3 | Exponential (1s, 2s, 4s) |
| CBS timeout | Yes | 3 | Exponential (1s, 2s, 4s) |
| Biller timeout | Yes | 3 | Exponential (1s, 2s, 4s) |
| Database deadlock | Yes | 5 | Linear (100ms) |
| Invalid account | No | 0 | - |
| Insufficient balance | No | 0 | - |
| Sanctions block | No | 0 | - |

### 12.2 Reversal Policy

| Transaction Type | Reversal Window | Compensation |
|-----------------|-----------------|--------------|
| Cash-In | 30 minutes | Credit customer, Debit float |
| Cash-Out | 30 minutes | Debit customer, Credit float |
| P2P Transfer | 30 minutes | Reverse debit/credit |
| Bill Payment | 24 hours | Call biller reversal API |

### 12.3 Saga Compensation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    REVERSAL FLOW                            │
│                                                              │
│  1. Original Saga:                                          │
│     A → B → C → D → E                                        │
│                                                              │
│  2. If Step D fails:                                         │
│     A → B → C → [D fails] → [compensate C] → [compensate B] │
│                              → [compensate A] → saga FAILED  │
│                                                              │
│  3. Compensation Order (reverse):                            │
│     Compensate E → Compensate D → Compensate C → Compensate B │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 12.4 Manual Review Queue

Transactions requiring manual intervention:

| Scenario | Action |
|----------|--------|
| Max retries exceeded | Queue for manual resolution |
| Suspicious transaction (AML) | Queue for compliance review |
| Reconciliation mismatch | Queue for operations review |
| System error with partial state | Queue for technical review |

---

## 13. Monitoring & Observability

### 13.1 Metrics (Micrometer)

**Transaction Metrics:**
```yaml
agentbanking:
  transactions:
    total:                    # Counter
      type: counter
      description: "Total number of transactions"
      tags: [type, status, agent_id]
    
    duration:                  # Timer
      type: timer
      description: "Transaction processing duration"
      tags: [type]
    
    amount:                    # Distribution summary
      type: summary
      description: "Transaction amount distribution"
      tags: [type]
```

**Saga Metrics:**
```yaml
agentbanking:
  saga:
    started:                   # Counter
    completed:                 # Counter
    failed:                    # Counter
    duration:                  # Timer
    step_duration:              # Timer
      tags: [saga_type, step_name]
```

**Infrastructure Metrics:**
```yaml
agentbanking:
  infrastructure:
    db:
      connections:             # Gauge
      pool_active:             # Gauge
      pool_idle:               # Gauge
    
    redis:
      connections:             # Gauge
      cache_hits:              # Counter
      cache_misses:            # Counter
    
    kafka:
      messages_sent:           # Counter
      messages_consumed:       # Counter
      consumer_lag:            # Gauge
```

### 13.2 Distributed Tracing (OpenTelemetry)

**Trace Context Propagation:**
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Gateway   │────▶│  Backend    │────▶│   Adapter   │
│   (Trace    │     │  Service    │     │   Service   │
│   Start)    │     │   (Span)    │     │   (Span)    │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │
   trace-id            trace-id            trace-id
   span-id-1           span-id-2           span-id-3
```

**Saga Trace:**
```
trace-id: abc123
├── span: saga-start
│   └── span: validate-transaction
│       └── span: lock-customer-account
│           └── span: debit-agent-float
│               └── span: credit-customer-account
│                   └── span: calculate-commission
│                       └── span: send-notifications
```

### 13.3 Logging Standards

**Log Format:**
```json
{
  "timestamp": "2026-04-06T10:30:00.123+08:00",
  "level": "INFO",
  "logger": "c.a.t.s.CashInService",
  "trace_id": "abc123",
  "span_id": "def456",
  "message": "Cash-in transaction completed",
  "agent_id": "AGT-20260406-001",
  "customer_id": "CUST-123456",
  "amount": 500.00,
  "transaction_id": "TXN-20260406-001"
}
```

**NEVER Log:**
- Full card numbers (PAN) — mask as `411111******1111`
- MyKad numbers — encrypted at rest, never in logs
- PIN blocks — NEVER log
- CVV/CVC — NEVER log
- Full account numbers — mask as `123456******7890`

### 13.4 Health Checks

**Liveness Probe:**
```yaml
GET /actuator/health/liveness
# Returns 200 if service is running
```

**Readiness Probe:**
```yaml
GET /actuator/health/readiness
# Returns 200 if all dependencies are available
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "cbsGateway": {"status": "UP"},
    "temporal": {"status": "UP"}
  }
}
```

---

## 14. Deployment Architecture

### 14.1 Container Architecture

```yaml
# docker-compose.yml (Development)

services:
  postgres-onboarding:
    image: postgres:16
    environment:
      POSTGRES_DB: onboarding_db
    ports:
      - "5432:5432"
  
  postgres-float:
    image: postgres:16
    environment:
      POSTGRES_DB: float_db
    ports:
      - "5433:5432"
  
  postgres-transaction:
    image: postgres:16
    environment:
      POSTGRES_DB: transaction_db
    ports:
      - "5434:5432"
  
  postgres-ledger:
    image: postgres:16
    environment:
      POSTGRES_DB: ledger_db
    ports:
      - "5435:5432"
  
  postgres-commission:
    image: postgres:16
    environment:
      POSTGRES_DB: commission_db
    ports:
      - "5436:5432"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports:
      - "9092:9092"
  
  temporal:
    image: temporalio/auto-setup:1.22.0
    ports:
      - "7233:7233"
    environment:
      - DB=postgres
      - DB_PORT=5432
  
  gateway:
    image: agentbanking/gateway:latest
    ports:
      - "8080:8080"
  
  onboarding-service:
    image: agentbanking/onboarding:latest
    ports:
      - "8081:8080"
  
  float-service:
    image: agentbanking/float:latest
    ports:
      - "8082:8080"
  
  transaction-service:
    image: agentbanking/transaction:latest
    ports:
      - "8083:8080"
```

### 14.2 EOD Settlement Scripts

```bash
#!/bin/bash
# scripts/eod-settlement.sh

# Run at EOD (23:00 MYT)
set -e

TIMESTAMP=$(date +%Y%m%d)
LOG_FILE="/var/log/settlement-${TIMESTAMP}.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Starting EOD settlement for ${TIMESTAMP}"

# 1. Freeze all pending transactions
psql -h postgres-ledger -U agentbanking -d ledger_db \
    -c "SELECT freeze_pending_transactions('${TIMESTAMP}')"

# 2. Calculate daily commissions
psql -h postgres-commission -U agentbanking -d commission_db \
    -c "SELECT calculate_daily_commissions('${TIMESTAMP}')"

# 3. Reconcile agent floats
psql -h postgres-float -U agentbanking -d float_db \
    -c "SELECT reconcile_agent_floats('${TIMESTAMP}')"

# 4. Generate settlement reports
psql -h postgres-settlement -U agentbanking -d settlement_db \
    -c "SELECT generate_settlement_reports('${TIMESTAMP}')"

# 5. Archive daily transactions
psql -h postgres-transaction -U agentbanking -d transaction_db \
    -c "SELECT archive_transactions('${TIMESTAMP}')"

log "EOD settlement completed for ${TIMESTAMP}"
```

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Agent | Third-party retail location offering banking services |
| Float | Working capital provided to agent for transactions |
| Saga | Pattern for managing distributed transactions with compensation |
| Temporal | Workflow orchestration engine implementing Saga pattern |
| HSM | Hardware Security Module for cryptographic operations |
| CBS | Core Banking System (bank's main accounting system) |
| ISO 8583 | Standard format for card payment transactions |

## Appendix B: Reference Documents

| Document | Location |
|----------|----------|
| BRD | `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-brd.md` |
| BDD | `docs/superpowers/specs/agent-banking-platform/2026-04-06-agent-banking-platform-bdd.md` |
| Architecture | `docs/ideas/Agent-ARCHITECTURE.md` |
| Domain Map | `docs/ideas/ARCH-supplementary/Microservices Domain Map.md` |
| API Spec | `docs/api/openapi.yaml` |

---

**End of Design Specification v2.0**
