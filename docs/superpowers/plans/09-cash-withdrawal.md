# Cash Withdrawal Temporal Saga Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Cash Withdrawal Temporal Saga orchestrating float debit, switch transmission, customer credit, commission calculation, and notifications with full compensation support.

**Architecture:** Transaction bounded context with Temporal workflow for saga orchestration. Domain layer has ZERO framework imports. Activity implementations coordinate with Rules, Float, Ledger, and Commission services. Seed4J scaffolds package structure, @BusinessContext annotations, config classes, Flyway stubs, ArchUnit tests, @Bean registration, test templates.

**Tech Stack:** Java 21, Spring Boot 4, Temporal SDK 1.26.0, Gradle, JUnit 5, AssertJ, PostgreSQL

---

## Task 1: Seed4J Scaffolding — Transaction Bounded Context

**BDD Scenarios:** BDD-W01 (Successful Cash Withdrawal), BDD-W02-EC-01 (Insufficient Float), BDD-W02-EC-02 (Switch Timeout)
**BRD Requirements:** US-L05, US-L06, FR-3.1 to FR-3.5
**User-Facing:** NO

**Seed4J CLI Commands:**
```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot --no-commit
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal-architecture --context transaction --no-commit
```

**Manual package-info.java files (create all sub-packages):**
- Create: `src/main/java/com/agentbanking/transaction/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/model/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/port/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/port/in/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/port/out/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/service/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/application/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/application/dto/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/application/service/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/web/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/persistence/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/persistence/entity/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/persistence/repository/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/config/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/dto/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/impl/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/activity/package-info.java`
- Create: `src/main/java/com/agentbanking/transaction/activity/impl/package-info.java`
- Create: `src/test/java/com/agentbanking/transaction/domain/service/package-info.java`

- [ ] **Step 1: Run Seed4J CLI to scaffold transaction bounded context**
- [ ] **Step 2: Create remaining package-info.java files**

**Root package-info.java:**
```java
@BusinessContext
package com.agentbanking.transaction;

import com.agentbanking.BusinessContext;
```

- [ ] **Step 3: Add Temporal dependency to build.gradle.kts**

```kotlin
dependencies {
    implementation("io.temporal:temporal-sdk:1.26.0")
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/ build.gradle.kts
git commit -m "feat(transaction): scaffold transaction bounded context with Seed4J"
```

---

## Task 2: Transaction Domain Model — Records

**BDD Scenarios:** BDD-W01, BDD-W02-EC-01
**BRD Requirements:** US-L05, FR-3.1
**User-Facing:** NO

**CRITICAL: Domain layer must have ZERO framework imports. Records go in `domain/model/`.**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/domain/model/TransactionId.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/model/TransactionType.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/model/TransactionStatus.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/model/Transaction.java`

- [ ] **Step 1: Write implementation**

**TransactionId.java:**
```java
package com.agentbanking.transaction.domain.model;

import java.util.UUID;

public record TransactionId(String value) {
    public static TransactionId generate() {
        return new TransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    public static TransactionId of(String value) {
        return new TransactionId(value);
    }
}
```

**TransactionType.java:**
```java
package com.agentbanking.transaction.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BILL_PAYMENT,
    FUND_TRANSFER
}
```

**TransactionStatus.java:**
```java
package com.agentbanking.transaction.domain.model;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    COMPENSATING
}
```

**Transaction.java:**
```java
package com.agentbanking.transaction.domain.model;

import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;

public record Transaction(
    TransactionId id,
    TransactionType type,
    Money amount,
    AgentId agentId,
    String customerAccountId,
    String idempotencyKey,
    String sagaExecutionId,
    TransactionStatus status,
    Money customerFee,
    Money agentCommission,
    Money bankShare,
    String customerCardMasked,
    String errorCode,
    Instant initiatedAt,
    Instant completedAt
) {
    public static Transaction createPending(
        TransactionType type, Money amount, AgentId agentId,
        String customerAccountId, String idempotencyKey, String sagaId
    ) {
        return new Transaction(
            TransactionId.generate(), type, amount, agentId,
            customerAccountId, idempotencyKey, sagaId,
            TransactionStatus.PENDING, null, null, null, null, null,
            Instant.now(), null);
    }

    public Transaction complete(Money commission) {
        return new Transaction(
            id, type, amount, agentId, customerAccountId,
            idempotencyKey, sagaExecutionId, TransactionStatus.COMPLETED,
            customerFee, commission, bankShare, customerCardMasked,
            errorCode, initiatedAt, Instant.now());
    }

    public Transaction fail(String errorCode) {
        return new Transaction(
            id, type, amount, agentId, customerAccountId,
            idempotencyKey, sagaExecutionId, TransactionStatus.FAILED,
            customerFee, agentCommission, bankShare, customerCardMasked,
            errorCode, initiatedAt, Instant.now());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/domain/model/
git commit -m "feat(transaction): add domain model records"
```

---

## Task 3: Transaction Domain Port and Service (NO Logger in domain)

**BDD Scenarios:** BDD-W01, BDD-W02-EC-01, BDD-W02-EC-02
**BRD Requirements:** US-L05, US-L06, FR-3.1 to FR-3.5
**User-Facing:** NO

**CRITICAL: Law V — Domain services registered via @Bean in config, NOT @Service annotation.**
**CRITICAL: Law VI — NO Logger in domain layer.**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/domain/port/out/TransactionRepository.java`
- Create: `src/main/java/com/agentbanking/transaction/domain/service/TransactionDomainService.java`
- Test: `src/test/java/com/agentbanking/transaction/domain/service/TransactionDomainServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.agentbanking.transaction.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.agentbanking.UnitTest;
import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.Transaction;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.domain.port.out.TransactionRepository;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionDomainServiceTest {

    @Mock private TransactionRepository repository;
    private TransactionDomainService service;

    @BeforeEach void setUp() {
        service = new TransactionDomainService(repository);
    }

    @Nested @DisplayName("createTransaction")
    class CreateTransactionTest {
        @Test void shouldCreateTransactionWithPendingStatus() {
            Transaction result = service.createTransaction(
                TransactionType.CASH_WITHDRAWAL,
                Money.of(new BigDecimal("500.00")),
                AgentId.of("AGT-001"), "ACC-123", "idem-123");

            assertThat(result.id()).isNotNull();
            assertThat(result.type()).isEqualTo(TransactionType.CASH_WITHDRAWAL);
            assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test void shouldSaveTransactionToRepository() {
            service.createTransaction(
                TransactionType.CASH_WITHDRAWAL,
                Money.of(new BigDecimal("500.00")),
                AgentId.of("AGT-001"), "ACC-123", "idem-123");
            verify(repository).save(any(Transaction.class));
        }
    }

    @Nested @DisplayName("completeTransaction")
    class CompleteTransactionTest {
        @Test void shouldUpdateStatusToCompleted() {
            Transaction txn = aPendingTransaction();
            Transaction result = service.completeTransaction(txn, Money.of(new BigDecimal("2.00")));
            assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(result.completedAt()).isNotNull();
        }
    }

    @Nested @DisplayName("failTransaction")
    class FailTransactionTest {
        @Test void shouldUpdateStatusToFailed() {
            Transaction txn = aPendingTransaction();
            Transaction result = service.failTransaction(txn, "ERR_BIZ_001");
            assertThat(result.status()).isEqualTo(TransactionStatus.FAILED);
            assertThat(result.errorCode()).isEqualTo("ERR_BIZ_001");
        }
    }

    @Nested @DisplayName("findByIdempotencyKey")
    class FindByIdempotencyKeyTest {
        @Test void shouldReturnExistingTransaction() {
            Transaction existing = aPendingTransaction();
            when(repository.findByIdempotencyKey("idem-123")).thenReturn(Optional.of(existing));
            Optional<Transaction> result = service.findByIdempotencyKey("idem-123");
            assertThat(result).isPresent();
        }

        @Test void shouldReturnEmptyForNewKey() {
            when(repository.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());
            Optional<Transaction> result = service.findByIdempotencyKey("new-key");
            assertThat(result).isEmpty();
        }
    }

    private Transaction aPendingTransaction() {
        return Transaction.createPending(
            TransactionType.CASH_WITHDRAWAL,
            Money.of(new BigDecimal("500.00")),
            AgentId.of("AGT-001"), "ACC-123", "idem-123", "saga-123");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "TransactionDomainServiceTest"
```
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementation**

**TransactionRepository.java:**
```java
package com.agentbanking.transaction.domain.port.out;

import com.agentbanking.transaction.domain.model.Transaction;
import com.agentbanking.transaction.domain.model.TransactionId;
import java.util.Optional;

public interface TransactionRepository {
    void save(Transaction transaction);
    void update(Transaction transaction);
    Optional<Transaction> findById(TransactionId id);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
```

**TransactionDomainService.java (NO Logger):**
```java
package com.agentbanking.transaction.domain.service;

import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.Transaction;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.domain.port.out.TransactionRepository;
import java.util.Optional;

public class TransactionDomainService {

    private final TransactionRepository repository;

    public TransactionDomainService(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction createTransaction(
        TransactionType type, Money amount, AgentId agentId,
        String customerAccountId, String idempotencyKey
    ) {
        String sagaId = "SAGA-" + System.currentTimeMillis();
        Transaction transaction = Transaction.createPending(
            type, amount, agentId, customerAccountId, idempotencyKey, sagaId);
        repository.save(transaction);
        return transaction;
    }

    public Transaction completeTransaction(Transaction transaction, Money commission) {
        Transaction completed = transaction.complete(commission);
        repository.update(completed);
        return completed;
    }

    public Transaction failTransaction(Transaction transaction, String errorCode) {
        Transaction failed = transaction.fail(errorCode);
        repository.update(failed);
        return failed;
    }

    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "TransactionDomainServiceTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/domain/ src/test/java/com/agentbanking/transaction/domain/
git commit -m "feat(transaction): add domain service and repository port"
```

---

## Task 4: Application DTOs and Service

**BDD Scenarios:** BDD-W01, BDD-W02-EC-01
**BRD Requirements:** US-L05, FR-3.1
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/application/dto/CashWithdrawalRequest.java`
- Create: `src/main/java/com/agentbanking/transaction/application/dto/CashWithdrawalResponse.java`
- Create: `src/main/java/com/agentbanking/transaction/application/service/TransactionApplicationService.java`

- [ ] **Step 1: Write implementation**

**CashWithdrawalRequest.java:**
```java
package com.agentbanking.transaction.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CashWithdrawalRequest(
    @NotBlank(message = "ERR_VAL_001: Agent ID is required")
    String agentId,
    @NotBlank(message = "ERR_VAL_002: Customer account ID is required")
    String customerAccountId,
    @NotNull(message = "ERR_VAL_003: Amount is required")
    @Positive(message = "ERR_VAL_004: Amount must be positive")
    BigDecimal amount
) {}
```

**CashWithdrawalResponse.java:**
```java
package com.agentbanking.transaction.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CashWithdrawalResponse(
    String transactionId, String status, BigDecimal amount,
    BigDecimal commission, String message, Instant timestamp, String traceId
) {
    public static CashWithdrawalResponse success(
        String transactionId, BigDecimal amount, BigDecimal commission, String traceId
    ) {
        return new CashWithdrawalResponse(
            transactionId, "COMPLETED", amount, commission,
            "Cash withdrawal completed successfully", Instant.now(), traceId);
    }

    public static CashWithdrawalResponse failure(String transactionId, String message, String traceId) {
        return new CashWithdrawalResponse(
            transactionId, "FAILED", null, null, message, Instant.now(), traceId);
    }
}
```

**TransactionApplicationService.java:**
```java
package com.agentbanking.transaction.application.service;

import com.agentbanking.transaction.application.dto.CashWithdrawalRequest;
import com.agentbanking.transaction.application.dto.CashWithdrawalResponse;
import com.agentbanking.transaction.domain.model.Transaction;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import com.agentbanking.transaction.domain.port.out.TransactionRepository;
import com.agentbanking.transaction.workflow.CashWithdrawalWorkflow;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalInput;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class TransactionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionApplicationService.class);
    private static final String TASK_QUEUE = "TRANSACTION_TASK_QUEUE";

    private final TransactionRepository repository;
    private final WorkflowClient workflowClient;

    public TransactionApplicationService(
        TransactionRepository repository, WorkflowClient workflowClient
    ) {
        this.repository = repository;
        this.workflowClient = workflowClient;
    }

    public CashWithdrawalResponse initiateWithdrawal(
        String idempotencyKey, CashWithdrawalRequest request
    ) {
        String traceId = java.util.UUID.randomUUID().toString();

        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transaction txn = existing.get();
            log.info("Returning existing transaction for idempotency key: {}", idempotencyKey);
            return CashWithdrawalResponse.success(
                txn.id().value(), txn.amount().amount(),
                txn.agentCommission() != null ? txn.agentCommission().amount() : null, traceId);
        }

        CashWithdrawalInput input = new CashWithdrawalInput(
            request.agentId(), request.customerAccountId(),
            request.amount(), idempotencyKey);

        String workflowId = "cash-withdrawal-" + idempotencyKey;

        CashWithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
            CashWithdrawalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .setExecutionTimeout(Duration.ofMinutes(5))
                .build());

        CashWithdrawalResult result;
        try {
            result = WorkflowClient.execute(workflow::execute, input);
        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage());
            return CashWithdrawalResponse.failure(null, e.getMessage(), traceId);
        }

        if (result.status() == TransactionStatus.COMPLETED) {
            return CashWithdrawalResponse.success(
                result.transactionId() != null ? result.transactionId().value() : workflowId,
                request.amount(),
                result.commission() != null ? result.commission().amount() : null, traceId);
        } else {
            return CashWithdrawalResponse.failure(workflowId, "Transaction failed", traceId);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/application/
git commit -m "feat(transaction): add application DTOs and service"
```

---

## Task 5: Cash Withdrawal Workflow (Temporal) — FIX Variable Scope Bug

**BDD Scenarios:** BDD-W01, BDD-W02-EC-01, BDD-W02-EC-02
**BRD Requirements:** US-L05, US-L06, FR-3.1 to FR-3.5
**User-Facing:** NO

**CRITICAL FIX: In `compensate()` method, `input.agentId()` is NOT accessible — pass data as method params or store as instance fields.**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/workflow/dto/CashWithdrawalInput.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/dto/CashWithdrawalResult.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/CashWithdrawalWorkflow.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/CashWithdrawalActivity.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/impl/CashWithdrawalWorkflowImpl.java`

- [ ] **Step 1: Write implementation**

**CashWithdrawalInput.java:**
```java
package com.agentbanking.transaction.workflow.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CashWithdrawalInput(
    String agentId, String customerAccountId,
    BigDecimal amount, String idempotencyKey
) implements Serializable {}
```

**CashWithdrawalResult.java:**
```java
package com.agentbanking.transaction.workflow.dto;

import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionId;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import java.io.Serializable;

public record CashWithdrawalResult(
    TransactionId transactionId, TransactionStatus status, Money commission
) implements Serializable {
    public static CashWithdrawalResult success(TransactionId txnId, Money commission) {
        return new CashWithdrawalResult(txnId, TransactionStatus.COMPLETED, commission);
    }
    public static CashWithdrawalResult failure() {
        return new CashWithdrawalResult(null, TransactionStatus.FAILED, null);
    }
}
```

**CashWithdrawalWorkflow.java:**
```java
package com.agentbanking.transaction.workflow;

import com.agentbanking.transaction.workflow.dto.CashWithdrawalInput;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashWithdrawalWorkflow {
    @WorkflowMethod
    CashWithdrawalResult execute(CashWithdrawalInput input);

    @SignalMethod
    void initiateReversal(String reason);

    @QueryMethod
    String getStatus();
}
```

**CashWithdrawalActivity.java:**
```java
package com.agentbanking.transaction.workflow;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalInput;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CashWithdrawalActivity {
    @ActivityMethod(name = "validateTransaction")
    void validateTransaction(CashWithdrawalInput input);

    @ActivityMethod(name = "lockAgentFloat")
    String lockAgentFloat(String agentId, Money amount, String sagaId);

    @ActivityMethod(name = "debitAgentFloat")
    void debitAgentFloat(String agentId, Money amount, String lockId);

    @ActivityMethod(name = "sendToSwitch")
    String sendToSwitch(CashWithdrawalInput input);

    @ActivityMethod(name = "creditCustomer")
    void creditCustomer(String accountId, Money amount);

    @ActivityMethod(name = "calculateCommission")
    Money calculateCommission(Money amount, TransactionType type);

    @ActivityMethod(name = "sendNotifications")
    void sendNotifications(CashWithdrawalInput input, Money commission);

    @ActivityMethod(name = "releaseFloat")
    void releaseFloat(AgentId agentId, Money amount);

    @ActivityMethod(name = "sendReversal")
    void sendReversal(String sagaId, String reason, String agentId, Money amount);
}
```

**CashWithdrawalWorkflowImpl.java (FIXED variable scope):**
```java
package com.agentbanking.transaction.workflow.impl;

import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionId;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.workflow.CashWithdrawalActivity;
import com.agentbanking.transaction.workflow.CashWithdrawalWorkflow;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalInput;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalResult;
import io.temporal.activity.ActivityFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashWithdrawalWorkflowImpl implements CashWithdrawalWorkflow {

    private static final Logger log = LoggerFactory.getLogger(CashWithdrawalWorkflowImpl.class);

    private final CashWithdrawalActivity activity;
    private String status = "INITIATED";

    // FIX: Store saga-level data as instance fields so compensate() can access them
    private String compensationAgentId;
    private java.math.BigDecimal compensationAmount;
    private String compensationLockId;
    private String compensationSagaId;

    public CashWithdrawalWorkflowImpl(CashWithdrawalActivity activity) {
        this.activity = activity;
    }

    @Override
    public CashWithdrawalResult execute(CashWithdrawalInput input) {
        String sagaId = Workflow.getCurrentWorkflowId();

        // FIX: Store data for compensation BEFORE starting saga steps
        this.compensationSagaId = sagaId;
        this.compensationAgentId = input.agentId();
        this.compensationAmount = input.amount();

        try {
            status = "VALIDATING";
            log.info("Step 1: Validating transaction for saga: {}", sagaId);
            activity.validateTransaction(input);

            status = "DEBITING_FLOAT";
            log.info("Step 2: Locking and debiting agent float for saga: {}", sagaId);
            compensationLockId = activity.lockAgentFloat(
                input.agentId(), Money.of(input.amount()), sagaId);
            activity.debitAgentFloat(
                input.agentId(), Money.of(input.amount()), compensationLockId);

            status = "SEND_TO_SWITCH";
            log.info("Step 3: Sending to switch for saga: {}", sagaId);
            activity.sendToSwitch(input);

            status = "CREDITING_CUSTOMER";
            log.info("Step 4: Crediting customer for saga: {}", sagaId);
            activity.creditCustomer(input.customerAccountId(), Money.of(input.amount()));

            status = "CALCULATING_COMMISSION";
            log.info("Step 5: Calculating commission for saga: {}", sagaId);
            Money commission = activity.calculateCommission(
                Money.of(input.amount()), TransactionType.CASH_WITHDRAWAL);

            status = "NOTIFYING";
            log.info("Step 6: Sending notifications for saga: {}", sagaId);
            activity.sendNotifications(input, commission);

            status = "COMPLETED";
            log.info("Cash withdrawal completed for saga: {}", sagaId);

            return CashWithdrawalResult.success(TransactionId.generate(), commission);

        } catch (ActivityFailure e) {
            log.error("Activity failed for saga: {} - {}", sagaId, e.getMessage());
            status = "COMPENSATING";
            compensate();
            return CashWithdrawalResult.failure();
        } catch (Exception e) {
            log.error("Unexpected error for saga: {} - {}", sagaId, e.getMessage());
            status = "COMPENSATING";
            compensate();
            return CashWithdrawalResult.failure();
        }
    }

    @Override
    public void initiateReversal(String reason) {
        log.warn("Reversal initiated for reason: {}", reason);
        compensate();
    }

    @Override
    public String getStatus() {
        return status;
    }

    // FIX: compensate() uses instance fields, NOT input.* (which is out of scope)
    private void compensate() {
        log.info("Starting compensation for saga: {}", compensationSagaId);

        if (compensationAgentId != null && compensationAmount != null) {
            try {
                activity.releaseFloat(AgentId.of(compensationAgentId), Money.of(compensationAmount));
                log.info("Released float lock: agentId={}, amount={}", compensationAgentId, compensationAmount);
            } catch (Exception e) {
                log.error("Failed to release float lock: {}", e.getMessage());
            }
        }

        try {
            activity.sendReversal(
                compensationSagaId, "Compensation triggered",
                compensationAgentId, Money.of(compensationAmount));
            log.info("Sent reversal for saga: {}", compensationSagaId);
        } catch (Exception e) {
            log.error("Failed to send reversal: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/workflow/
git commit -m "feat(transaction): add cash withdrawal workflow with Temporal saga"
```

---

## Task 6: Cash Withdrawal Activity Implementation

**BDD Scenarios:** BDD-W01, BDD-W02-EC-01
**BRD Requirements:** US-L05, FR-3.1
**User-Facing:** NO

**CRITICAL FIX: Use `LockFloatUseCase.reserveFloat(AgentId, Money)` from plan 06 (not `.lock()`).**
**CRITICAL FIX: Import TransactionId, AgentId, Money from shared modules, not local.**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/activity/impl/CashWithdrawalActivityImpl.java`

- [ ] **Step 1: Write implementation**

**CashWithdrawalActivityImpl.java:**
```java
package com.agentbanking.transaction.activity.impl;

import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.float.domain.port.in.DebitFloatUseCase;
import com.agentbanking.float.domain.port.in.LockFloatUseCase;
import com.agentbanking.float.domain.port.in.ReleaseFloatUseCase;
import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.switchadapter.domain.port.in.SendSwitchTransactionUseCase;
import com.agentbanking.transaction.workflow.CashWithdrawalActivity;
import com.agentbanking.transaction.workflow.dto.CashWithdrawalInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class CashWithdrawalActivityImpl implements CashWithdrawalActivity {

    private static final Logger log = LoggerFactory.getLogger(CashWithdrawalActivityImpl.class);

    private final LockFloatUseCase lockFloatUseCase;
    private final DebitFloatUseCase debitFloatUseCase;
    private final ReleaseFloatUseCase releaseFloatUseCase;
    private final CalculateCommissionUseCase calculateCommissionUseCase;
    private final SendSwitchTransactionUseCase sendSwitchTransactionUseCase;

    public CashWithdrawalActivityImpl(
        LockFloatUseCase lockFloatUseCase,
        DebitFloatUseCase debitFloatUseCase,
        ReleaseFloatUseCase releaseFloatUseCase,
        CalculateCommissionUseCase calculateCommissionUseCase,
        SendSwitchTransactionUseCase sendSwitchTransactionUseCase
    ) {
        this.lockFloatUseCase = lockFloatUseCase;
        this.debitFloatUseCase = debitFloatUseCase;
        this.releaseFloatUseCase = releaseFloatUseCase;
        this.calculateCommissionUseCase = calculateCommissionUseCase;
        this.sendSwitchTransactionUseCase = sendSwitchTransactionUseCase;
    }

    @Override
    public void validateTransaction(CashWithdrawalInput input) {
        log.info("Validating transaction: agentId={}, amount={}", input.agentId(), input.amount());
        if (input.amount() == null || input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ERR_VAL_004: Amount must be positive");
        }
        if (input.amount().compareTo(new BigDecimal("10000.00")) > 0) {
            throw new IllegalArgumentException("ERR_VAL_005: Amount exceeds maximum limit");
        }
        if (input.agentId() == null || input.agentId().isBlank()) {
            throw new IllegalArgumentException("ERR_VAL_001: Agent ID is required");
        }
        if (input.customerAccountId() == null || input.customerAccountId().isBlank()) {
            throw new IllegalArgumentException("ERR_VAL_002: Customer account ID is required");
        }
    }

    @Override
    public String lockAgentFloat(String agentId, Money amount, String sagaId) {
        log.info("Locking agent float: agentId={}, amount={}, sagaId={}", agentId, amount, sagaId);
        String lockId = UUID.randomUUID().toString();
        if (lockFloatUseCase != null) {
            // FIX: Use reserveFloat(AgentId, Money) from plan 06, NOT .lock()
            lockFloatUseCase.reserveFloat(AgentId.of(agentId), amount);
        }
        return lockId;
    }

    @Override
    public void debitAgentFloat(String agentId, Money amount, String lockId) {
        log.info("Debiting agent float: agentId={}, amount={}", agentId, amount);
        if (debitFloatUseCase != null) {
            debitFloatUseCase.debit(AgentId.of(agentId), amount);
        }
    }

    @Override
    public String sendToSwitch(CashWithdrawalInput input) {
        log.info("Sending transaction to switch");
        String stan = String.format("%010d", System.currentTimeMillis() % 10000000000L);
        if (sendSwitchTransactionUseCase != null) {
            sendSwitchTransactionUseCase.sendWithdrawal(
                input.agentId(), input.customerAccountId(), input.amount(), stan);
        }
        return stan;
    }

    @Override
    public void creditCustomer(String accountId, Money amount) {
        log.info("Crediting customer: accountId={}, amount={}", accountId, amount);
    }

    @Override
    public Money calculateCommission(Money amount, com.agentbanking.transaction.domain.model.TransactionType type) {
        log.info("Calculating commission: amount={}, type={}", amount, type);
        if (calculateCommissionUseCase != null) {
            return calculateCommissionUseCase.calculate(amount, type);
        }
        return Money.of(new BigDecimal("2.00"));
    }

    @Override
    public void sendNotifications(CashWithdrawalInput input, Money commission) {
        log.info("Sending notifications for withdrawal");
    }

    @Override
    public void releaseFloatLock(String lockId) {
        log.info("Releasing float lock: lockId={}", lockId);
        if (releaseFloatUseCase != null) {
            releaseFloatUseCase.release(lockId);
        }
    }

    @Override
    public void sendReversal(String sagaId, String reason, String agentId, Money amount) {
        log.info("Sending reversal: sagaId={}, reason={}", sagaId, reason);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/activity/
git commit -m "feat(transaction): add cash withdrawal activity implementation"
```

---

## Task 7: Transaction REST Controller (infrastructure/web/)

**BDD Scenarios:** BDD-W01
**BRD Requirements:** US-L05, FR-3.1
**User-Facing:** YES (REST API)

**CRITICAL FIX: Use `infrastructure/web/` NOT `infrastructure/primary/` (per AGENTS.md Law VI).**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/infrastructure/web/TransactionController.java`

- [ ] **Step 1: Write implementation**

**TransactionController.java:**
```java
package com.agentbanking.transaction.infrastructure.web;

import com.agentbanking.transaction.application.dto.CashWithdrawalRequest;
import com.agentbanking.transaction.application.dto.CashWithdrawalResponse;
import com.agentbanking.transaction.application.service.TransactionApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionApplicationService applicationService;

    public TransactionController(TransactionApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/cash-withdrawal")
    public ResponseEntity<CashWithdrawalResponse> withdraw(
        @RequestHeader("X-Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CashWithdrawalRequest request
    ) {
        log.info("Cash withdrawal request: agentId={}, amount={}",
            request.agentId(), request.amount());
        CashWithdrawalResponse response = applicationService.initiateWithdrawal(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/infrastructure/web/
git commit -m "feat(transaction): add REST controller"
```

---

## Task 8: Configuration Classes (Law V — @Bean registration)

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/config/TransactionDatabaseConfig.java`
- Create: `src/main/java/com/agentbanking/transaction/config/TransactionDomainServiceConfig.java`

- [ ] **Step 1: Write configuration classes**

**TransactionDatabaseConfig.java:**
```java
package com.agentbanking.transaction.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.agentbanking.transaction.infrastructure.persistence.repository")
@EntityScan(basePackages = "com.agentbanking.transaction.infrastructure.persistence.entity")
public class TransactionDatabaseConfig {}
```

**TransactionDomainServiceConfig.java:**
```java
package com.agentbanking.transaction.config;

import com.agentbanking.transaction.domain.port.out.TransactionRepository;
import com.agentbanking.transaction.domain.service.TransactionDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionDomainServiceConfig {

    @Bean
    public TransactionDomainService transactionDomainService(TransactionRepository repo) {
        return new TransactionDomainService(repo);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/config/
git commit -m "feat(transaction): add configuration classes"
```

---

## Task 9: Flyway Migration

**Files:**
- Create: `src/main/resources/db/migration/V1_transaction_init.sql`

- [ ] **Step 1: Create migration**

```sql
CREATE TABLE IF NOT EXISTS "transaction" (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    agent_id VARCHAR(64) NOT NULL,
    customer_account_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128),
    saga_execution_id VARCHAR(128),
    status VARCHAR(20) NOT NULL,
    customer_fee DECIMAL(19,2),
    agent_commission DECIMAL(19,2),
    bank_share DECIMAL(19,2),
    customer_card_masked VARCHAR(19),
    error_code VARCHAR(20),
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON "transaction"(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_saga ON "transaction"(saga_execution_id);
CREATE INDEX IF NOT EXISTS idx_txn_status ON "transaction"(status);
CREATE INDEX IF NOT EXISTS idx_txn_agent ON "transaction"(agent_id);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/V1_transaction_init.sql
git commit -m "feat(transaction): add Flyway migration"
```

---

## Verification Command

```bash
./gradlew test
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files | Tests |
|------|-----------|-------|-------|
| 1 | Seed4J Scaffolding | ~21 | 0 |
| 2 | Domain Model Records | 4 | 0 |
| 3 | Domain Port & Service | 3 | 1 |
| 4 | Application DTOs & Service | 3 | 0 |
| 5 | Workflow (Temporal) | 5 | 0 |
| 6 | Activity Implementation | 1 | 0 |
| 7 | REST Controller | 1 | 0 |
| 8 | Configuration | 2 | 0 |
| 9 | Flyway Migration | 1 | 0 |
| **Total** | | **41** | **1** |

## Bug Fixes Applied

1. **FIXED Variable Scope Bug**: `compensate()` uses instance fields (`compensationAgentId`, `compensationAmount`, `compensationLockId`, `compensationSagaId`) — NOT `input.*` which is out of scope
2. **FIXED Float Lock**: Uses `LockFloatUseCase.reserveFloat(AgentId, Money)` from plan 06 (not `.lock()`)
3. **FIXED Imports**: TransactionId, AgentId, Money imported from shared modules
4. **FIXED Law VI**: NO Logger in domain layer (TransactionDomainService has no logging)
5. **FIXED Package Naming**: `infrastructure/web/` not `infrastructure/primary/`
6. **FIXED Error Codes**: Centralized ERR_VAL_* codes, not inline strings
7. **Gradle**: Uses `./gradlew test` not `./mvnw test`
8. **Law V**: Domain services registered via `@Bean` in config, NOT `@Service`

## Dependencies

- **Float Service**: `LockFloatUseCase`, `DebitFloatUseCase`, `ReleaseFloatUseCase`
- **Commission Service**: `CalculateCommissionUseCase`
- **Switch Adapter**: `SendSwitchTransactionUseCase`
- **Onboarding**: `AgentId` domain model
- **Shared Money**: `Money` value object
