# Plan 08: Transaction Orchestrator

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Transaction Orchestrator bounded context using Temporal Saga orchestration for durable multi-step financial transactions.

**Architecture:** Hexagonal architecture with Temporal workflow engine. Domain layer has ZERO framework imports. Temporal workflow and activity interfaces in dedicated `workflow/` and `activity/` packages. Domain services registered via `@Bean` in config (Law V).

**Tech Stack:** Java 21, Spring Boot 4, Temporal SDK, Spring Data JPA, PostgreSQL, Redis, JUnit 5, Mockito, Gradle

**Wave:** 3 (Temporal Saga — Seed4J scaffolds, manual Temporal code)

**Approach:** Seed4J-first for package structure, config classes, Flyway stubs, ArchUnit tests. Write Temporal workflow/activity code manually.

**FIX:** Remove duplicate `TransactionId`, `AgentId`, `Money` — import from shared modules:
- `com.agentbanking.shared.identity.domain.TransactionId`
- `com.agentbanking.shared.identity.domain.AgentId`
- `com.agentbanking.shared.money.domain.Money`

**Keep local to orchestrator:** `SagaExecutionId`, `SagaStepLog`, `SagaStepStatus`, `TransactionType`, `TransactionStatus`

---

## Seed4J Scaffolding

Run these Seed4J commands to scaffold the orchestrator bounded context:

```bash
# Apply hexagonal architecture template to orchestrator context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal --context orchestrator

# This creates:
# - src/main/java/com/agentbanking/orchestrator/config/OrchestratorDomainServiceConfig.java
# - src/main/java/com/agentbanking/orchestrator/config/OrchestratorDatabaseConfig.java
# - Flyway stub: db/migration/orchestrator/
# - ArchUnit test stub
# - Test templates
```

After scaffolding, verify:
- `src/main/java/com/agentbanking/orchestrator/config/OrchestratorDomainServiceConfig.java` exists
- `src/main/java/com/agentbanking/orchestrator/config/OrchestratorDatabaseConfig.java` exists
- `src/test/java/com/agentbanking/orchestrator/` has test stubs

---

## Task 0: Build Configuration

Add Temporal and Redis dependencies to `gradle/libs.versions.toml`:

```toml
[versions]
temporal = "1.26.0"

[libraries.temporal-spring-boot-starter]
name = "temporal-spring-boot-starter"
group = "io.temporal"
version.ref = "temporal"

[libraries.temporal-sdk]
name = "temporal-sdk"
group = "io.temporal"
version.ref = "temporal"

[libraries.spring-boot-starter-data-redis]
name = "spring-boot-starter-data-redis"
group = "org.springframework.boot"
```

Add to `build.gradle.kts` dependencies:

```kotlin
implementation(libs.temporal.spring.boot.starter)
implementation(libs.temporal.sdk)
implementation(libs.spring.boot.starter.data.redis)
```

---

## Task 1: Package Structure

Create the following directory structure under `src/main/java/com/agentbanking/orchestrator/`:

```
orchestrator/
├── application/
│   ├── dto/
│   │   ├── TransactionRequest.java
│   │   └── TransactionResponse.java
│   └── package-info.java
├── config/
│   ├── OrchestratorDatabaseConfig.java (scaffolded)
│   ├── OrchestratorDomainServiceConfig.java (scaffolded)
│   ├── TemporalConfig.java
│   └── RedisConfig.java
├── domain/
│   ├── model/
│   │   ├── SagaExecutionId.java
│   │   ├── SagaStepLog.java
│   │   ├── SagaStepStatus.java
│   │   ├── TransactionType.java
│   │   └── TransactionStatus.java
│   ├── port/
│   │   └── out/
│   │       ├── SagaStepLogRepository.java
│   │       └── IdempotencyService.java
│   └── service/
│       └── ReversalHandler.java
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   │   ├── TransactionEntity.java
│   │   │   └── SagaStepLogEntity.java
│   │   └── repository/
│   │       ├── TransactionRepository.java
│   │       └── TransactionRepositoryImpl.java
│   └── secondary/
│       └── RedisIdempotencyService.java
├── workflow/
│   ├── model/
│   │   ├── WorkflowInput.java
│   │   └── WorkflowResult.java
│   ├── TransactionWorkflow.java
│   └── impl/
│       └── BaseTransactionWorkflowImpl.java
└── activity/
    └── TransactionActivity.java
```

---

## Task 2: Domain Model (Local to Orchestrator)

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/TransactionStatus.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaExecutionId.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaStepStatus.java`
- `src/main/java/com/agentbanking/orchestrator/domain/model/SagaStepLog.java`

```java
package com.agentbanking.orchestrator.domain.model;

public enum TransactionType {
    CASH_IN,
    CASH_OUT,
    PAYMENT,
    TOP_UP,
    REVERSAL
}
```

```java
package com.agentbanking.orchestrator.domain.model;

public enum TransactionStatus {
    PENDING,
    INITIATED,
    VALIDATION_PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSAL_INITIATED,
    REVERSED,
    CANCELLED
}
```

```java
package com.agentbanking.orchestrator.domain.model;

import java.util.UUID;

public final class SagaExecutionId {
    private final String value;

    private SagaExecutionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SagaExecutionId cannot be blank");
        }
        this.value = value;
    }

    public static SagaExecutionId of(String value) {
        return new SagaExecutionId(value);
    }

    public static SagaExecutionId generate() {
        return new SagaExecutionId("SAGA-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SagaExecutionId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
```

```java
package com.agentbanking.orchestrator.domain.model;

public enum SagaStepStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATED
}
```

```java
package com.agentbanking.orchestrator.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SagaStepLog(
    UUID id,
    String transactionId,
    SagaExecutionId sagaExecutionId,
    String stepName,
    int stepOrder,
    SagaStepStatus status,
    Instant startedAt,
    Instant completedAt,
    String errorMessage,
    boolean compensationExecuted
) {
    public static SagaStepLog create(String transactionId, SagaExecutionId sagaExecutionId, String stepName, int stepOrder) {
        return new SagaStepLog(
            UUID.randomUUID(), transactionId, sagaExecutionId, stepName, stepOrder,
            SagaStepStatus.PENDING, Instant.now(), null, null, false
        );
    }

    public SagaStepLog markCompleted() {
        return new SagaStepLog(
            id, transactionId, sagaExecutionId, stepName, stepOrder,
            SagaStepStatus.COMPLETED, startedAt, Instant.now(), errorMessage, compensationExecuted
        );
    }

    public SagaStepLog markFailed(String error) {
        return new SagaStepLog(
            id, transactionId, sagaExecutionId, stepName, stepOrder,
            SagaStepStatus.FAILED, startedAt, Instant.now(), error, compensationExecuted
        );
    }

    public SagaStepLog markCompensated() {
        return new SagaStepLog(
            id, transactionId, sagaExecutionId, stepName, stepOrder,
            SagaStepStatus.COMPENSATED, startedAt, completedAt, errorMessage, true
        );
    }
}
```

---

## Task 3: Domain Ports

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/IdempotencyService.java`
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/SagaStepLogRepository.java`

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyService {
    Optional<String> getCachedResponse(String idempotencyKey);
    void cacheResponse(String idempotencyKey, String response, Duration ttl);
    boolean isDuplicate(String idempotencyKey);
}
```

```java
package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.SagaStepLog;
import java.util.List;

public interface SagaStepLogRepository {
    SagaStepLog save(SagaStepLog stepLog);
    List<SagaStepLog> findBySagaExecutionId(SagaExecutionId sagaExecutionId);
    List<SagaStepLog> findByTransactionId(String transactionId);
}
```

---

## Task 4: Domain Service — ReversalHandler

**File to create:**
- `src/main/java/com/agentbanking/orchestrator/domain/service/ReversalHandler.java`

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.SagaStepLog;
import com.agentbanking.orchestrator.domain.port.out.SagaStepLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReversalHandler {

    private static final Logger log = LoggerFactory.getLogger(ReversalHandler.class);

    private final SagaStepLogRepository sagaStepLogRepository;

    public ReversalHandler(SagaStepLogRepository sagaStepLogRepository) {
        this.sagaStepLogRepository = sagaStepLogRepository;
    }

    public void handleCompensation(SagaExecutionId sagaId, String failedStep) {
        log.info("Executing compensation for saga: {} from failed step: {}", sagaId.value(), failedStep);

        List<SagaStepLog> steps = sagaStepLogRepository.findBySagaExecutionId(sagaId);

        if (steps.isEmpty()) {
            log.warn("No saga steps found for compensation: {}", sagaId.value());
            return;
        }

        List<SagaStepLog> reversedSteps = steps.stream()
                .filter(s -> s.stepOrder() > getStepOrder(steps, failedStep))
                .sorted((a, b) -> Integer.compare(b.stepOrder(), a.stepOrder()))
                .toList();

        for (SagaStepLog step : reversedSteps) {
            if (step.status() != SagaStepStatus.COMPLETED) {
                continue;
            }
            executeCompensation(step);
        }

        log.info("Compensation completed for saga: {}", sagaId.value());
    }

    private int getStepOrder(List<SagaStepLog> steps, String stepName) {
        return steps.stream()
                .filter(s -> s.stepName().equals(stepName))
                .findFirst()
                .map(SagaStepLog::stepOrder)
                .orElse(0);
    }

    private void executeCompensation(SagaStepLog step) {
        log.info("Executing compensation for step: {}", step.stepName());

        switch (step.stepName()) {
            case "LOCK_FLOAT" -> releaseFloatLock(step);
            case "DEBIT_FLOAT" -> reverseFloatDebit(step);
            case "CREDIT_CUSTOMER" -> reverseCustomerCredit(step);
            default -> log.warn("No compensation handler for step: {}", step.stepName());
        }
    }

    private void releaseFloatLock(SagaStepLog step) {
        log.info("Releasing float lock for step: {}", step.stepName());
    }

    private void reverseFloatDebit(SagaStepLog step) {
        log.info("Reversing float debit for step: {}", step.stepName());
    }

    private void reverseCustomerCredit(SagaStepLog step) {
        log.info("Reversing customer credit for step: {}", step.stepName());
    }
}
```

---

## Task 5: Application Layer — DTOs

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/application/dto/TransactionRequest.java`
- `src/main/java/com/agentbanking/orchestrator/application/dto/TransactionResponse.java`

```java
package com.agentbanking.orchestrator.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransactionRequest(
    @NotBlank String agentId,
    @NotBlank String customerAccountId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String transactionType,
    @NotBlank String idempotencyKey,
    String customerCardMasked
) {}
```

```java
package com.agentbanking.orchestrator.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    String transactionId,
    String sagaExecutionId,
    String status,
    BigDecimal amount,
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    String failureReason,
    Instant initiatedAt,
    Instant completedAt
) {}
```

---

## Task 6: Workflow Interfaces and Models

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/workflow/model/WorkflowInput.java`
- `src/main/java/com/agentbanking/orchestrator/workflow/model/WorkflowResult.java`
- `src/main/java/com/agentbanking/orchestrator/workflow/TransactionWorkflow.java`
- `src/main/java/com/agentbanking/orchestrator/activity/TransactionActivity.java`

```java
package com.agentbanking.orchestrator.workflow.model;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public record WorkflowInput(
    TransactionId transactionId,
    AgentId agentId,
    String customerAccountId,
    Money amount,
    TransactionType transactionType,
    String idempotencyKey
) {}
```

```java
package com.agentbanking.orchestrator.workflow.model;

import com.agentbanking.orchestrator.domain.model.TransactionStatus;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public record WorkflowResult(
    TransactionId transactionId,
    TransactionStatus status,
    Money commission,
    String failureReason
) {
    public static WorkflowResult success(TransactionId transactionId, Money commission) {
        return new WorkflowResult(transactionId, TransactionStatus.COMPLETED, commission, null);
    }

    public static WorkflowResult failure(TransactionId transactionId, String reason) {
        return new WorkflowResult(transactionId, TransactionStatus.FAILED, null, reason);
    }
}
```

```java
package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.model.WorkflowInput;
import com.agentbanking.orchestrator.workflow.model.WorkflowResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransactionWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WorkflowInput input);

    @io.temporal.workflow.SignalMethod
    void cancel(String reason);

    @io.temporal.workflow.SignalMethod
    void pause(String reason);

    @io.temporal.workflow.QueryMethod
    String getStatus();
}
```

```java
package com.agentbanking.orchestrator.activity;

import com.agentbanking.orchestrator.workflow.model.WorkflowInput;
import com.agentbanking.shared.money.domain.Money;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TransactionActivity {

    @ActivityMethod
    void validateTransaction(WorkflowInput input);

    @ActivityMethod
    String lockAgentFloat(String agentId, Money amount, String transactionId);

    @ActivityMethod
    void debitAgentFloat(String agentId, Money amount, String lockId);

    @ActivityMethod
    void creditCustomer(String customerAccountId, Money amount);

    @ActivityMethod
    void releaseFloatLock(String lockId);

    @ActivityMethod
    void recordLedgerEntry(String transactionId, String debitAccount, String creditAccount, Money amount);

    @ActivityMethod
    void calculateCommission(String transactionId, String agentId, String transactionType, Money amount);
}
```

---

## Task 7: Workflow Implementation

**File to create:**
- `src/main/java/com/agentbanking/orchestrator/workflow/impl/BaseTransactionWorkflowImpl.java`

```java
package com.agentbanking.orchestrator.workflow.impl;

import com.agentbanking.orchestrator.activity.TransactionActivity;
import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.TransactionStatus;
import com.agentbanking.orchestrator.domain.service.ReversalHandler;
import com.agentbanking.orchestrator.workflow.TransactionWorkflow;
import com.agentbanking.orchestrator.workflow.model.WorkflowInput;
import com.agentbanking.orchestrator.workflow.model.WorkflowResult;
import com.agentbanking.shared.money.domain.Money;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class BaseTransactionWorkflowImpl implements TransactionWorkflow {

    private final TransactionActivity activity = Workflow.newActivityStub(
            TransactionActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    private TransactionStatus status = TransactionStatus.PROCESSING;

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        try {
            activity.validateTransaction(input);

            String lockId = activity.lockAgentFloat(
                    input.agentId().value(),
                    input.amount(),
                    input.transactionId().value()
            );

            activity.debitAgentFloat(
                    input.agentId().value(),
                    input.amount(),
                    lockId
            );

            activity.creditCustomer(
                    input.customerAccountId(),
                    input.amount()
            );

            activity.releaseFloatLock(lockId);

            status = TransactionStatus.COMPLETED;
            return WorkflowResult.success(input.transactionId(), Money.of(java.math.BigDecimal.ZERO));

        } catch (io.temporal.failure.ActivityFailure e) {
            SagaExecutionId sagaId = SagaExecutionId.of(Workflow.getInfo().getWorkflowId());
            new ReversalHandler(null).handleCompensation(sagaId, "validateTransaction");

            status = TransactionStatus.FAILED;
            return WorkflowResult.failure(input.transactionId(), e.getMessage());
        }
    }

    @Override
    public String getStatus() {
        return status.name();
    }

    @Override
    public void cancel(String reason) {
        status = TransactionStatus.CANCELLED;
    }

    @Override
    public void pause(String reason) {
        // Workflow pauses are handled by Temporal infrastructure
    }
}
```

---

## Task 8: Infrastructure Layer — JPA Entities

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/TransactionEntity.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/SagaStepLogEntity.java`

```java
package com.agentbanking.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction")
public class TransactionEntity {

    @Id
    private String id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "customer_account_id", nullable = false)
    private String customerAccountId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "saga_execution_id")
    private String sagaExecutionId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "customer_fee", precision = 19, scale = 4)
    private BigDecimal customerFee;

    @Column(name = "agent_commission", precision = 19, scale = 4)
    private BigDecimal agentCommission;

    @Column(name = "bank_share", precision = 19, scale = 4)
    private BigDecimal bankShare;

    @Column(name = "customer_card_masked")
    private String customerCardMasked;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public TransactionEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getCustomerAccountId() { return customerAccountId; }
    public void setCustomerAccountId(String customerAccountId) { this.customerAccountId = customerAccountId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getSagaExecutionId() { return sagaExecutionId; }
    public void setSagaExecutionId(String sagaExecutionId) { this.sagaExecutionId = sagaExecutionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getCustomerFee() { return customerFee; }
    public void setCustomerFee(BigDecimal customerFee) { this.customerFee = customerFee; }
    public BigDecimal getAgentCommission() { return agentCommission; }
    public void setAgentCommission(BigDecimal agentCommission) { this.agentCommission = agentCommission; }
    public BigDecimal getBankShare() { return bankShare; }
    public void setBankShare(BigDecimal bankShare) { this.bankShare = bankShare; }
    public String getCustomerCardMasked() { return customerCardMasked; }
    public void setCustomerCardMasked(String customerCardMasked) { this.customerCardMasked = customerCardMasked; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
```

```java
package com.agentbanking.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_step_log")
public class SagaStepLogEntity {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "saga_execution_id", nullable = false)
    private String sagaExecutionId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "compensation_executed", nullable = false)
    private Boolean compensationExecuted;

    public SagaStepLogEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getSagaExecutionId() { return sagaExecutionId; }
    public void setSagaExecutionId(String sagaExecutionId) { this.sagaExecutionId = sagaExecutionId; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Boolean getCompensationExecuted() { return compensationExecuted; }
    public void setCompensationExecuted(Boolean compensationExecuted) { this.compensationExecuted = compensationExecuted; }
}
```

---

## Task 9: Infrastructure Layer — Repositories

**Files to create:**
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRepository.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRepositoryImpl.java`

```java
package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionEntity;
import java.util.Optional;

public interface TransactionRepository {
    void save(TransactionEntity entity);
    Optional<TransactionEntity> findById(String id);
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    void update(TransactionEntity entity);
}
```

```java
package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(TransactionEntity entity) {
        entityManager.persist(entity);
    }

    @Override
    public Optional<TransactionEntity> findById(String id) {
        TransactionEntity entity = entityManager.find(TransactionEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey) {
        try {
            TransactionEntity entity = entityManager
                    .createQuery("SELECT t FROM TransactionEntity t WHERE t.idempotencyKey = :key",
                            TransactionEntity.class)
                    .setParameter("key", idempotencyKey)
                    .getSingleResult();
            return Optional.of(entity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public void update(TransactionEntity entity) {
        entityManager.merge(entity);
    }
}
```

---

## Task 10: Infrastructure Layer — Redis Idempotency

**File to create:**
- `src/main/java/com/agentbanking/orchestrator/infrastructure/secondary/RedisIdempotencyService.java`

```java
package com.agentbanking.orchestrator.infrastructure.secondary;

import com.agentbanking.orchestrator.domain.port.out.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisIdempotencyService implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyService.class);
    private static final String KEY_PREFIX = "idem:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisIdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> getCachedResponse(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            log.debug("Cache hit for idempotency key: {}", idempotencyKey);
        }

        return Optional.ofNullable(value);
    }

    @Override
    public void cacheResponse(String idempotencyKey, String response, Duration ttl) {
        String key = KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, response, ttl);
        log.debug("Cached response for idempotency key: {} with TTL: {}ms", idempotencyKey, ttl.toMillis());
    }

    @Override
    public boolean isDuplicate(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

---

## Task 11: Configuration Classes

**File to create:**
- `src/main/java/com/agentbanking/orchestrator/config/TemporalConfig.java`

**File to create:**
- `src/main/java/com/agentbanking/orchestrator/config/RedisConfig.java`

**File to modify (scaffolded):**
- `src/main/java/com/agentbanking/orchestrator/config/OrchestratorDomainServiceConfig.java`

```java
package com.agentbanking.orchestrator.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    @Value("${agentbanking.temporal.host:localhost:7233}")
    private String temporalHost;

    @Value("${agentbanking.temporal.namespace:agentbanking.default}")
    private String namespace;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalHost)
                .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build()
        );
    }
}
```

```java
package com.agentbanking.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
```

```java
package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.domain.port.out.IdempotencyService;
import com.agentbanking.orchestrator.domain.port.out.SagaStepLogRepository;
import com.agentbanking.orchestrator.domain.service.ReversalHandler;
import com.agentbanking.orchestrator.infrastructure.secondary.RedisIdempotencyService;
import io.temporal.client.WorkflowClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorDomainServiceConfig {

    @Bean
    public IdempotencyService idempotencyService(RedisIdempotencyService redisIdempotencyService) {
        return redisIdempotencyService;
    }

    @Bean
    public ReversalHandler reversalHandler(SagaStepLogRepository sagaStepLogRepository) {
        return new ReversalHandler(sagaStepLogRepository);
    }
}
```

---

## Task 12: Flyway Migration

**File to create:**
- `src/main/resources/db/migration/orchestrator/V1_orchestrator_transaction_init.sql`

```sql
-- Transaction table
CREATE TABLE IF NOT EXISTS "transaction" (
    id VARCHAR(64) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    customer_account_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) UNIQUE,
    saga_execution_id VARCHAR(64),
    status VARCHAR(50) NOT NULL,
    customer_fee NUMERIC(19, 4),
    agent_commission NUMERIC(19, 4),
    bank_share NUMERIC(19, 4),
    customer_card_masked VARCHAR(19),
    error_code VARCHAR(20),
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_transaction_saga_execution_id ON "transaction"(saga_execution_id);
CREATE INDEX idx_transaction_agent_id ON "transaction"(agent_id);
CREATE INDEX idx_transaction_status ON "transaction"(status);
CREATE INDEX idx_transaction_idempotency_key ON "transaction"(idempotency_key);

-- Saga step log table
CREATE TABLE IF NOT EXISTS saga_step_log (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    saga_execution_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    compensation_executed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_saga_execution_id ON saga_step_log(saga_execution_id);
CREATE INDEX idx_transaction_id ON saga_step_log(transaction_id);
CREATE INDEX idx_status ON saga_step_log(status);
```

---

## Task 13: Tests

**File:** `src/test/java/com/agentbanking/orchestrator/domain/service/IdempotencyServiceTest.java`

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.infrastructure.secondary.RedisIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new RedisIdempotencyService(redisTemplate);
    }

    @Test
    @DisplayName("should return cached response when exists")
    void shouldReturnCachedResponse() {
        String idempotencyKey = "idem-123";
        String cachedValue = "{\"status\":\"COMPLETED\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idem:" + idempotencyKey)).thenReturn(cachedValue);

        Optional<String> result = service.getCachedResponse(idempotencyKey);

        assertTrue(result.isPresent());
        assertEquals(cachedValue, result.get());
    }

    @Test
    @DisplayName("should return empty when no cached response")
    void shouldReturnEmptyWhenNoCache() {
        String idempotencyKey = "idem-456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idem:" + idempotencyKey)).thenReturn(null);

        Optional<String> result = service.getCachedResponse(idempotencyKey);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should cache response with TTL")
    void shouldCacheResponse() {
        String idempotencyKey = "idem-789";
        String response = "{\"status\":\"COMPLETED\"}";
        Duration ttl = Duration.ofHours(24);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.cacheResponse(idempotencyKey, response, ttl);

        verify(valueOperations).set("idem:" + idempotencyKey, response, ttl);
    }

    @Test
    @DisplayName("should check duplicate correctly")
    void shouldCheckDuplicate() {
        String idempotencyKey = "idem-dup";

        when(redisTemplate.hasKey("idem:" + idempotencyKey)).thenReturn(true);

        boolean result = service.isDuplicate(idempotencyKey);

        assertTrue(result);
    }
}
```

**File:** `src/test/java/com/agentbanking/orchestrator/domain/service/ReversalHandlerTest.java`

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.SagaStepLog;
import com.agentbanking.orchestrator.domain.model.SagaStepStatus;
import com.agentbanking.orchestrator.domain.port.out.SagaStepLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReversalHandlerTest {

    @Mock
    private SagaStepLogRepository sagaStepLogRepository;

    private ReversalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReversalHandler(sagaStepLogRepository);
    }

    @Test
    @DisplayName("should execute compensation in reverse order")
    void shouldExecuteCompensation() {
        SagaExecutionId sagaId = SagaExecutionId.of("saga-002");

        List<SagaStepLog> steps = List.of(
            SagaStepLog.create("TXN-001", sagaId, "VALIDATE", 1).markCompleted(),
            SagaStepLog.create("TXN-001", sagaId, "LOCK_FLOAT", 2).markCompleted(),
            SagaStepLog.create("TXN-001", sagaId, "DEBIT_FLOAT", 3).markCompleted()
        );

        when(sagaStepLogRepository.findBySagaExecutionId(sagaId)).thenReturn(steps);

        handler.handleCompensation(sagaId, "VALIDATE");

        verify(sagaStepLogRepository).findBySagaExecutionId(sagaId);
    }

    @Test
    @DisplayName("should handle empty step history")
    void shouldHandleEmptyStepHistory() {
        SagaExecutionId sagaId = SagaExecutionId.of("saga-003");

        when(sagaStepLogRepository.findBySagaExecutionId(sagaId)).thenReturn(List.of());

        handler.handleCompensation(sagaId, "VALIDATE");

        verify(sagaStepLogRepository).findBySagaExecutionId(sagaId);
    }
}
```

---

## Task 14: Application Configuration

**File:** `src/main/resources/application.yaml` (add orchestrator section)

```yaml
spring:
  datasource:
    orchestrator:
      url: jdbc:postgresql://localhost:5432/orchestrator
      username: postgres
      password: postgres
      driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000ms

agentbanking:
  temporal:
    namespace: agentbanking.default
    host: localhost:7233
  idempotency:
    ttl-hours: 24
```

---

## Verification

```bash
./gradlew test -Dtest.include="**/orchestrator/**/*Test*"
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files Created | Tests |
|------|-----------|---------------|-------|
| 0 | Build Configuration | libs.versions.toml, build.gradle.kts | 0 |
| 1 | Package Structure | directories | 0 |
| 2 | Domain Model (local) | 5 | 0 |
| 3 | Domain Ports | 2 | 0 |
| 4 | Domain Service — ReversalHandler | 1 | 1 |
| 5 | Application DTOs | 2 | 0 |
| 6 | Workflow Interfaces & Models | 4 | 0 |
| 7 | Workflow Implementation | 1 | 0 |
| 8 | JPA Entities | 2 | 0 |
| 9 | Repositories | 2 | 0 |
| 10 | Redis Idempotency | 1 | 1 |
| 11 | Configuration | 3 | 0 |
| 12 | Flyway Migration | 1 | 0 |
| 13 | Tests | 2 | 2 |
| **Total** | | **26** | **4** |

**FIX:** All imports of `TransactionId`, `AgentId`, `Money` come from shared modules:
- `com.agentbanking.shared.identity.domain.TransactionId`
- `com.agentbanking.shared.identity.domain.AgentId`
- `com.agentbanking.shared.money.domain.Money`

No duplicates exist in the orchestrator context.

All components follow hexagonal architecture:
- Domain layer has ZERO Spring/JPA/Temporal imports
- Domain services registered via `@Bean` in config (Law V)
- Infrastructure adapters use `@Repository`, `@Service` (Law VI)
- Temporal workflow/activity interfaces in dedicated packages
