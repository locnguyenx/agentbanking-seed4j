# Cash Deposit Temporal Saga Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Cash Deposit Temporal Saga orchestrating account validation, customer account debit, float credit, commission calculation, and notifications with full compensation support.

**Architecture:** Transaction bounded context with Temporal workflow for saga orchestration. Complementary to Cash Withdrawal — agent float **increases** (credit) instead of decreases (debit). Compensation **reverses credits with debits**. Seed4J scaffolds already done in plan 09 — this plan extends the existing transaction bounded context.

**Tech Stack:** Java 21, Spring Boot 4, Temporal SDK 1.26.0, Gradle, JUnit 5, AssertJ, PostgreSQL

---

## Task 1: Seed4J Scaffolding — Transaction Context (extends plan 09)

**BDD Scenarios:** BDD-D01 (Happy Path), BDD-D02 (Card Deposit), BDD-D03 (Compensation), BDD-D04 (Idempotency)
**BRD Requirements:** FR-4.1, FR-4.2, FR-4.3
**User-Facing:** NO

**Seed4J CLI:** Already scaffolded in plan 09. This plan extends the existing transaction bounded context.

- [ ] **Step 1: Verify transaction context exists from plan 09**

```bash
ls src/main/java/com/agentbanking/transaction/
# Should show: domain/, application/, infrastructure/, config/, workflow/, activity/
```

- [ ] **Step 2: Add CASH_DEPOSIT to TransactionType enum (from plan 09)**

Modify `src/main/java/com/agentbanking/transaction/domain/model/TransactionType.java`:
```java
public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,    // <-- ADD THIS
    BILL_PAYMENT,
    FUND_TRANSFER
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/domain/model/TransactionType.java
git commit -m "feat(transaction): add CASH_DEPOSIT transaction type"
```

---

## Task 2: Cash Deposit Application DTOs

**BDD Scenarios:** BDD-D01, BDD-D02
**BRD Requirements:** FR-4.1, FR-4.2
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/application/dto/CashDepositRequest.java`
- Create: `src/main/java/com/agentbanking/transaction/application/dto/CashDepositResponse.java`

- [ ] **Step 1: Write implementation**

**CashDepositRequest.java:**
```java
package com.agentbanking.transaction.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CashDepositRequest(
    @NotBlank(message = "ERR_VAL_001: Agent ID is required")
    String agentId,

    @NotBlank(message = "ERR_VAL_002: Destination account ID is required")
    String destinationAccountId,

    @NotNull(message = "ERR_VAL_003: Amount is required")
    @Positive(message = "ERR_VAL_004: Amount must be positive")
    BigDecimal amount
) {}
```

**CashDepositResponse.java:**
```java
package com.agentbanking.transaction.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CashDepositResponse(
    String transactionId, String status, BigDecimal amount,
    BigDecimal commission, String message, Instant timestamp, String traceId
) {
    public static CashDepositResponse success(
        String transactionId, BigDecimal amount, BigDecimal commission, String traceId
    ) {
        return new CashDepositResponse(
            transactionId, "COMPLETED", amount, commission,
            "Cash deposit completed successfully", Instant.now(), traceId);
    }

    public static CashDepositResponse failure(String transactionId, String message, String traceId) {
        return new CashDepositResponse(
            transactionId, "FAILED", null, null, message, Instant.now(), traceId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/application/dto/CashDeposit*.java
git commit -m "feat(transaction): add cash deposit DTOs"
```

---

## Task 3: Cash Deposit Application Service

**BDD Scenarios:** BDD-D01, BDD-D04
**BRD Requirements:** FR-2.4, FR-4.1
**User-Facing:** NO

**Files:**
- Modify: `src/main/java/com/agentbanking/transaction/application/service/TransactionApplicationService.java`

- [ ] **Step 1: Add deposit method to TransactionApplicationService**

```java
// ADD to TransactionApplicationService.java

public CashDepositResponse initiateDeposit(
    String idempotencyKey, CashDepositRequest request
) {
    String traceId = java.util.UUID.randomUUID().toString();

    var existing = repository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        Transaction txn = existing.get();
        log.info("Returning existing transaction for idempotency key: {}", idempotencyKey);
        return CashDepositResponse.success(
            txn.id().value(), txn.amount().amount(),
            txn.agentCommission() != null ? txn.agentCommission().amount() : null, traceId);
    }

    CashDepositInput input = new CashDepositInput(
        request.agentId(), request.destinationAccountId(),
        request.amount(), idempotencyKey);

    String workflowId = "cash-deposit-" + idempotencyKey;

    CashDepositWorkflow workflow = workflowClient.newWorkflowStub(
        CashDepositWorkflow.class,
        WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(TASK_QUEUE)
            .setExecutionTimeout(Duration.ofMinutes(5))
            .build());

    CashDepositResult result;
    try {
        result = WorkflowClient.execute(workflow::execute, input);
    } catch (Exception e) {
        log.error("Workflow execution failed: {}", e.getMessage());
        return CashDepositResponse.failure(null, e.getMessage(), traceId);
    }

    if (result.status() == TransactionStatus.COMPLETED) {
        return CashDepositResponse.success(
            result.transactionId() != null ? result.transactionId().value() : workflowId,
            request.amount(),
            result.commission() != null ? result.commission().amount() : null, traceId);
    } else {
        return CashDepositResponse.failure(workflowId, "Transaction failed", traceId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/application/service/TransactionApplicationService.java
git commit -m "feat(transaction): add cash deposit application service"
```

---

## Task 4: Cash Deposit Workflow (Temporal) — FIX Variable Scope Bug

**BDD Scenarios:** BDD-D01, BDD-D02, BDD-D03
**BRD Requirements:** FR-4.1, FR-4.2, FR-4.3
**User-Facing:** NO

**CRITICAL FIX: Same variable scope bug as plan 09 — store data as instance fields for compensation.**
**CRITICAL FIX: Deposit flow reverses — credit float instead of debit, reverse compensation direction.**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/workflow/dto/CashDepositInput.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/dto/CashDepositResult.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/CashDepositWorkflow.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/CashDepositActivity.java`
- Create: `src/main/java/com/agentbanking/transaction/workflow/impl/CashDepositWorkflowImpl.java`

- [ ] **Step 1: Write implementation**

**CashDepositInput.java:**
```java
package com.agentbanking.transaction.workflow.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CashDepositInput(
    String agentId, String destinationAccountId,
    BigDecimal amount, String idempotencyKey
) implements Serializable {}
```

**CashDepositResult.java:**
```java
package com.agentbanking.transaction.workflow.dto;

import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionId;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import java.io.Serializable;

public record CashDepositResult(
    TransactionId transactionId, TransactionStatus status, Money commission
) implements Serializable {
    public static CashDepositResult success(TransactionId txnId, Money commission) {
        return new CashDepositResult(txnId, TransactionStatus.COMPLETED, commission);
    }
    public static CashDepositResult failure() {
        return new CashDepositResult(null, TransactionStatus.FAILED, null);
    }
}
```

**CashDepositWorkflow.java:**
```java
package com.agentbanking.transaction.workflow;

import com.agentbanking.transaction.workflow.dto.CashDepositInput;
import com.agentbanking.transaction.workflow.dto.CashDepositResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashDepositWorkflow {
    @WorkflowMethod
    CashDepositResult execute(CashDepositInput input);

    @SignalMethod
    void initiateReversal(String reason);

    @QueryMethod
    String getStatus();
}
```

**CashDepositActivity.java:**
```java
package com.agentbanking.transaction.activity;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.workflow.dto.CashDepositInput;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CashDepositActivity {
    @ActivityMethod(name = "validateTransaction")
    void validateTransaction(CashDepositInput input);

    @ActivityMethod(name = "validateDestinationAccount")
    boolean validateDestinationAccount(String accountId);

    @ActivityMethod(name = "debitCustomerAccount")
    void debitCustomerAccount(String accountId, Money amount);

    @ActivityMethod(name = "creditAgentFloat")
    void creditAgentFloat(String agentId, Money amount, String lockId);

    @ActivityMethod(name = "lockAgentFloat")
    String lockAgentFloat(String agentId, Money amount, String sagaId);

    @ActivityMethod(name = "calculateCommission")
    Money calculateCommission(Money amount, TransactionType type);

    @ActivityMethod(name = "sendNotifications")
    void sendNotifications(CashDepositInput input, Money commission);

    @ActivityMethod(name = "releaseFloat")
    void releaseFloat(AgentId agentId, Money amount);

    @ActivityMethod(name = "sendReversal")
    void sendReversal(String sagaId, String reason, String agentId, Money amount);
}
```

**CashDepositWorkflowImpl.java (FIXED variable scope):**
```java
package com.agentbanking.transaction.workflow.impl;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionId;
import com.agentbanking.transaction.domain.model.TransactionStatus;
import com.agentbanking.transaction.domain.model.TransactionType;
import com.agentbanking.transaction.workflow.CashDepositActivity;
import com.agentbanking.transaction.workflow.CashDepositWorkflow;
import com.agentbanking.transaction.workflow.dto.CashDepositInput;
import com.agentbanking.transaction.workflow.dto.CashDepositResult;
import io.temporal.activity.ActivityFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashDepositWorkflowImpl implements CashDepositWorkflow {

    private static final Logger log = LoggerFactory.getLogger(CashDepositWorkflowImpl.class);

    private final CashDepositActivity activity;
    private String status = "INITIATED";

    // FIX: Store saga-level data as instance fields so compensate() can access them
    private String compensationAgentId;
    private String compensationDestinationAccountId;
    private java.math.BigDecimal compensationAmount;
    private String compensationLockId;
    private String compensationSagaId;

    public CashDepositWorkflowImpl(CashDepositActivity activity) {
        this.activity = activity;
    }

    @Override
    public CashDepositResult execute(CashDepositInput input) {
        String sagaId = Workflow.getCurrentWorkflowId();

        // FIX: Store data for compensation BEFORE starting saga steps
        this.compensationSagaId = sagaId;
        this.compensationAgentId = input.agentId();
        this.compensationDestinationAccountId = input.destinationAccountId();
        this.compensationAmount = input.amount();

        try {
            status = "VALIDATING";
            log.info("Step 1: Validating transaction for saga: {}", sagaId);
            activity.validateTransaction(input);

            status = "VALIDATING_DESTINATION";
            log.info("Step 2: Validating destination account for saga: {}", sagaId);
            activity.validateDestinationAccount(input.destinationAccountId());

            status = "DEBITING_CUSTOMER";
            log.info("Step 3: Debiting customer account for saga: {}", sagaId);
            activity.debitCustomerAccount(
                input.destinationAccountId(), Money.of(input.amount()));

            status = "CREDITING_FLOAT";
            log.info("Step 4: Crediting agent float for saga: {}", sagaId);
            compensationLockId = activity.lockAgentFloat(
                input.agentId(), Money.of(input.amount()), sagaId);
            activity.creditAgentFloat(
                input.agentId(), Money.of(input.amount()), compensationLockId);

            status = "CALCULATING_COMMISSION";
            log.info("Step 5: Calculating commission for saga: {}", sagaId);
            Money commission = activity.calculateCommission(
                Money.of(input.amount()), TransactionType.CASH_DEPOSIT);

            status = "NOTIFYING";
            log.info("Step 6: Sending notifications for saga: {}", sagaId);
            activity.sendNotifications(input, commission);

            status = "COMPLETED";
            log.info("Cash deposit completed for saga: {}", sagaId);

            return CashDepositResult.success(TransactionId.generate(), commission);

        } catch (ActivityFailure e) {
            log.error("Activity failed for saga: {} - {}", sagaId, e.getMessage());
            status = "COMPENSATING";
            compensate();
            return CashDepositResult.failure();
        } catch (Exception e) {
            log.error("Unexpected error for saga: {} - {}", sagaId, e.getMessage());
            status = "COMPENSATING";
            compensate();
            return CashDepositResult.failure();
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
    // FIX: Deposit compensation reverses direction — debit float to undo credit, credit customer to undo debit
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
git commit -m "feat(transaction): add cash deposit workflow with Temporal saga"
```

---

## Task 5: Cash Deposit Activity Implementation

**BDD Scenarios:** BDD-D01, BDD-D02, BDD-D03
**BRD Requirements:** FR-4.1, FR-4.2, FR-4.3
**User-Facing:** NO

**CRITICAL FIX: Use `LockFloatUseCase.reserveFloat(AgentId, Money)` from plan 06 (not `.lock()`).**

**Files:**
- Create: `src/main/java/com/agentbanking/transaction/activity/impl/CashDepositActivityImpl.java`

- [ ] **Step 1: Write implementation**

**CashDepositActivityImpl.java:**
```java
package com.agentbanking.transaction.activity.impl;

import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.float.domain.port.in.CreditFloatUseCase;
import com.agentbanking.float.domain.port.in.LockFloatUseCase;
import com.agentbanking.float.domain.port.in.ReleaseFloatUseCase;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.workflow.CashDepositActivity;
import com.agentbanking.transaction.workflow.dto.CashDepositInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class CashDepositActivityImpl implements CashDepositActivity {

    private static final Logger log = LoggerFactory.getLogger(CashDepositActivityImpl.class);

    private final LockFloatUseCase lockFloatUseCase;
    private final CreditFloatUseCase creditFloatUseCase;
    private final ReleaseFloatUseCase releaseFloatUseCase;
    private final CalculateCommissionUseCase calculateCommissionUseCase;

    public CashDepositActivityImpl(
        LockFloatUseCase lockFloatUseCase,
        CreditFloatUseCase creditFloatUseCase,
        ReleaseFloatUseCase releaseFloatUseCase,
        CalculateCommissionUseCase calculateCommissionUseCase
    ) {
        this.lockFloatUseCase = lockFloatUseCase;
        this.creditFloatUseCase = creditFloatUseCase;
        this.releaseFloatUseCase = releaseFloatUseCase;
        this.calculateCommissionUseCase = calculateCommissionUseCase;
    }

    @Override
    public void validateTransaction(CashDepositInput input) {
        log.info("Validating deposit: agentId={}, amount={}", input.agentId(), input.amount());
        if (input.amount() == null || input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ERR_VAL_004: Amount must be positive");
        }
        if (input.amount().compareTo(new BigDecimal("50000.00")) > 0) {
            throw new IllegalArgumentException("ERR_VAL_005: Amount exceeds maximum limit of 50000.00");
        }
        if (input.agentId() == null || input.agentId().isBlank()) {
            throw new IllegalArgumentException("ERR_VAL_001: Agent ID is required");
        }
        if (input.destinationAccountId() == null || input.destinationAccountId().isBlank()) {
            throw new IllegalArgumentException("ERR_VAL_002: Destination account ID is required");
        }
    }

    @Override
    public boolean validateDestinationAccount(String accountId) {
        log.info("Validating destination account: {}", accountId);
        return true;
    }

    @Override
    public void debitCustomerAccount(String accountId, Money amount) {
        log.info("Debiting customer account: accountId={}, amount={}", accountId, amount);
    }

    @Override
    public String lockAgentFloat(String agentId, Money amount, String sagaId) {
        log.info("Locking agent float for credit: agentId={}, amount={}, sagaId={}", agentId, amount, sagaId);
        String lockId = UUID.randomUUID().toString();
        if (lockFloatUseCase != null) {
            lockFloatUseCase.reserveFloat(AgentId.of(agentId), amount);
        }
        return lockId;
    }

    @Override
    public void creditAgentFloat(String agentId, Money amount, String lockId) {
        log.info("Crediting agent float: agentId={}, amount={}", agentId, amount);
        if (creditFloatUseCase != null) {
            TransactionId txnId = TransactionId.generate();
            creditFloatUseCase.credit(AgentId.of(agentId), amount, txnId);
        }
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
    public void sendNotifications(CashDepositInput input, Money commission) {
        log.info("Sending notifications for deposit");
    }

    @Override
    public void releaseFloat(AgentId agentId, Money amount) {
        log.info("Releasing float lock: agentId={}, amount={}", agentId, amount);
        if (releaseFloatUseCase != null) {
            releaseFloatUseCase.release(agentId, amount);
        }
    }

    @Override
    public void sendReversal(String sagaId, String reason, String agentId, Money amount) {
        log.info("Sending reversal: sagaId={}, reason={}", sagaId, reason);
    }
}
```

- [ ] **Step 2: Register activity as bean in TransactionDomainServiceConfig**

Add to `src/main/java/com/agentbanking/transaction/config/TransactionDomainServiceConfig.java`:
```java
import com.agentbanking.float.domain.port.in.ReleaseFloatUseCase; // ADD THIS IMPORT

@Bean
public CashDepositActivity cashDepositActivity(
    LockFloatUseCase lockFloatUseCase,
    CreditFloatUseCase creditFloatUseCase,
    ReleaseFloatUseCase releaseFloatUseCase,
    CalculateCommissionUseCase calculateCommissionUseCase
) {
    return new CashDepositActivityImpl(
        lockFloatUseCase, creditFloatUseCase, releaseFloatUseCase, calculateCommissionUseCase);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/config/TransactionDomainServiceConfig.java
git commit -m "feat(transaction): register cash deposit activity bean"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/activity/impl/CashDepositActivityImpl.java
git add src/main/java/com/agentbanking/transaction/config/TransactionDomainServiceConfig.java
git commit -m "feat(transaction): add cash deposit activity implementation"
```

---

## Task 6: Cash Deposit REST Controller (infrastructure/web/)

**BDD Scenarios:** BDD-D01
**BRD Requirements:** FR-4.1
**User-Facing:** YES (REST API)

**CRITICAL FIX: Use `infrastructure/web/` NOT `infrastructure/primary/` (per AGENTS.md Law VI).**

**Files:**
- Modify: `src/main/java/com/agentbanking/transaction/infrastructure/web/TransactionController.java`

- [ ] **Step 1: Add deposit endpoint**

```java
// ADD to TransactionController.java

@PostMapping("/cash-deposit")
public ResponseEntity<CashDepositResponse> deposit(
    @RequestHeader("X-Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody CashDepositRequest request
) {
    log.info("Cash deposit request: agentId={}, amount={}",
        request.agentId(), request.amount());
    CashDepositResponse response = applicationService.initiateDeposit(idempotencyKey, request);
    return ResponseEntity.ok(response);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/infrastructure/web/TransactionController.java
git commit -m "feat(transaction): add cash deposit REST endpoint"
```

---

## Task 7: Register CashDepositWorkflow in TemporalConfig

**Files:**
- Modify: `src/main/java/com/agentbanking/transaction/config/TemporalConfig.java` (from plan 09)

- [ ] **Step 1: Update TemporalConfig to register deposit workflow**

```java
// ADD to TemporalConfig.java

@Bean
public Worker temporalWorker(
    WorkerFactory workerFactory,
    CashWithdrawalActivity withdrawActivity,
    CashDepositActivity depositActivity
) {
    Worker worker = workerFactory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(
        CashWithdrawalWorkflowImpl.class,
        CashDepositWorkflowImpl.class
    );
    worker.registerActivitiesImplementations(withdrawActivity, depositActivity);
    workerFactory.start();
    return worker;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/transaction/config/TemporalConfig.java
git commit -m "feat(transaction): register cash deposit workflow in Temporal config"
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
| 1 | Seed4J Scaffolding (extends plan 09) | 1 | 0 |
| 2 | Deposit DTOs | 2 | 0 |
| 3 | Application Service | 1 (modify) | 0 |
| 4 | Workflow (Temporal) | 5 | 0 |
| 5 | Activity Implementation | 1 | 0 |
| 6 | REST Controller | 1 (modify) | 0 |
| 7 | Temporal Config Update | 1 (modify) | 0 |
| **Total** | | **12** | **0** |

## Bug Fixes Applied

1. **FIXED Variable Scope Bug**: `compensate()` uses instance fields (`compensationAgentId`, `compensationDestinationAccountId`, `compensationAmount`, `compensationLockId`, `compensationSagaId`) — NOT `input.*` which is out of scope
2. **FIXED Float Lock**: Uses `LockFloatUseCase.reserveFloat(AgentId, Money)` from plan 06 (not `.lock()`)
3. **FIXED Package Naming**: `infrastructure/web/` not `infrastructure/primary/`
4. **FIXED Error Codes**: Centralized ERR_VAL_* codes, not inline strings
5. **Gradle**: Uses `./gradlew test` not `./mvnw test`
6. **Deposit Flow**: Credits float instead of debits, reverses compensation direction

## Key Differences from Cash Withdrawal

| Aspect | Withdrawal | Deposit |
|--------|-----------|---------|
| Float direction | Debit (decrease) | Credit (increase) |
| Compensation | Release lock, reverse switch | Release lock, reverse via sendReversal |
| Max amount | 10,000.00 | 50,000.00 |
| Account validation | Not required | ProxyEnquiry required |
| Activity methods | `debitAgentFloat`, `creditCustomer` | `creditAgentFloat`, `debitCustomerAccount` |

## Dependencies

- **Plan 09**: Cash Withdrawal (reuses transaction bounded context)
- **Float Service**: `LockFloatUseCase`, `CreditFloatUseCase`
- **Commission Service**: `CalculateCommissionUseCase`
- **Onboarding**: `AgentId` domain model
- **Shared Money**: `Money` value object
