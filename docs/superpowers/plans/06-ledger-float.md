# Plan 06: Ledger & Float Service

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Agent Float Management and Double-Entry Ledger bounded contexts with PESSIMISTIC_WRITE locks, hexagonal architecture, and reservation-based float locking for cross-service saga coordination.

**Architecture:** Two bounded contexts (float, ledger) following hexagonal architecture. Float manages agent wallet balances with reservation support; Ledger records double-entry journal entries. Both use database-per-service pattern with PostgreSQL. Domain services registered via `@Bean` in config (Law V).

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Gradle

**Wave:** 2 (Normal Service — no Temporal)

**Approach:** Seed4J-first. Use Seed4J CLI to scaffold both contexts. Write custom business logic manually.

**CRITICAL CROSS-PLAN:** Plans 09 and 10 will call `LockFloatUseCase.reserveFloat()` and `ReleaseFloatUseCase.releaseReservation()`. These interfaces MUST be stable and return/use reservation IDs as strings.

---

## Seed4J Scaffolding

Run these Seed4J commands to scaffold both bounded contexts:

```bash
# Apply hexagonal architecture template to float context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal --context float

# Apply hexagonal architecture template to ledger context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal --context ledger

# These create:
# - src/main/java/com/agentbanking/float/config/FloatDomainServiceConfig.java
# - src/main/java/com/agentbanking/float/config/FloatDatabaseConfig.java
# - src/main/java/com/agentbanking/ledger/config/LedgerDomainServiceConfig.java
# - src/main/java/com/agentbanking/ledger/config/LedgerDatabaseConfig.java
# - Flyway stubs: db/migration/float/, db/migration/ledger/
# - ArchUnit test stubs
# - Test templates
```

After scaffolding, verify:
- `src/main/java/com/agentbanking/float/config/FloatDomainServiceConfig.java` exists
- `src/main/java/com/agentbanking/ledger/config/LedgerDomainServiceConfig.java` exists
- `src/test/java/com/agentbanking/float/` and `src/test/java/com/agentbanking/ledger/` have test stubs

---

## Task 1: Shared Domain Model — Money and Identity

**NOTE:** These shared value objects are created in Plan 02 (domain model sub-plan). If they already exist, skip this task. If not, create them now.

**Files (only if not already present):**
- `src/main/java/com/agentbanking/shared/money/domain/Money.java`
- `src/main/java/com/agentbanking/shared/identity/domain/AgentId.java`
- `src/main/java/com/agentbanking/shared/identity/domain/TransactionId.java`

**Money.java** (must include `multiply` method for Plan 05):
```java
package com.agentbanking.shared.money.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class Money implements Comparable<Money> {
    private final BigDecimal amount;
    private final Currency currency;

    public static final Currency MYR = Currency.getInstance("MYR");

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, MYR);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, MYR);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        assertCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP), currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLessThan(Money other) {
        assertCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    private void assertCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return 31 * amount.hashCode() + currency.hashCode();
    }

    @Override
    public int compareTo(Money o) {
        assertCurrency(o);
        return amount.compareTo(o.amount);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + " " + amount.toPlainString();
    }
}
```

**AgentId.java:**
```java
package com.agentbanking.shared.identity.domain;

public final class AgentId {
    private final String value;

    private AgentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AgentId cannot be blank");
        }
        this.value = value;
    }

    public static AgentId of(String value) {
        return new AgentId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentId agentId)) return false;
        return value.equals(agentId.value);
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

**TransactionId.java:**
```java
package com.agentbanking.shared.identity.domain;

import java.util.UUID;

public final class TransactionId {
    private final String value;

    private TransactionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TransactionId cannot be blank");
        }
        this.value = value;
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionId that)) return false;
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

---

## Task 2: Float Bounded Context — Domain Layer

**Files to create:**
- `src/main/java/com/agentbanking/float/domain/model/AgentFloat.java`
- `src/main/java/com/agentbanking/float/domain/model/FloatTransaction.java`
- `src/main/java/com/agentbanking/float/domain/model/FloatTransactionType.java`
- `src/main/java/com/agentbanking/float/domain/port/out/AgentFloatRepository.java`
- `src/main/java/com/agentbanking/float/domain/port/in/BalanceInquiryUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/DebitFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/CreditFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/LockFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/port/in/ReleaseFloatUseCase.java`
- `src/main/java/com/agentbanking/float/domain/service/FloatService.java`

**AgentFloat.java:**
```java
package com.agentbanking.float.domain.model;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;

public final class AgentFloat {
    private final AgentId agentId;
    private final Money balance;
    private final Money reservedBalance;
    private final String currency;
    private final Instant updatedAt;
    private final long version;

    private AgentFloat(AgentId agentId, Money balance, Money reservedBalance, String currency, Instant updatedAt, long version) {
        this.agentId = agentId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.currency = currency;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static AgentFloat create(AgentId agentId, Money initialBalance) {
        return new AgentFloat(
            agentId,
            initialBalance,
            Money.zero(),
            "MYR",
            Instant.now(),
            0L
        );
    }

    public static AgentFloat restore(AgentId agentId, Money balance, Money reservedBalance, String currency, Instant updatedAt, long version) {
        return new AgentFloat(agentId, balance, reservedBalance, currency, updatedAt, version);
    }

    public AgentId getAgentId() { return agentId; }
    public Money getBalance() { return balance; }
    public Money getReservedBalance() { return reservedBalance; }
    public Money getAvailableBalance() { return balance.subtract(reservedBalance); }
    public String getCurrency() { return currency; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public boolean hasAvailableBalance(Money amount) {
        return getAvailableBalance().isGreaterThanOrEqual(amount);
    }

    public AgentFloat debit(Money amount) {
        if (!hasAvailableBalance(amount)) {
            throw new InsufficientFloatException("Insufficient float: available=" + getAvailableBalance() + ", requested=" + amount);
        }
        return new AgentFloat(
            agentId,
            balance.subtract(amount),
            reservedBalance,
            currency,
            Instant.now(),
            version + 1
        );
    }

    public AgentFloat credit(Money amount) {
        return new AgentFloat(
            agentId,
            balance.add(amount),
            reservedBalance,
            currency,
            Instant.now(),
            version + 1
        );
    }

    public AgentFloat reserve(Money amount) {
        if (!hasAvailableBalance(amount)) {
            throw new InsufficientFloatException("Cannot reserve: insufficient available float");
        }
        return new AgentFloat(
            agentId,
            balance,
            reservedBalance.add(amount),
            currency,
            Instant.now(),
            version + 1
        );
    }

    public AgentFloat releaseReservation(Money amount) {
        Money newReserved = reservedBalance.subtract(amount);
        if (newReserved.amount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot release more than reserved");
        }
        return new AgentFloat(
            agentId,
            balance,
            newReserved,
            currency,
            Instant.now(),
            version + 1
        );
    }

    public static class InsufficientFloatException extends RuntimeException {
        public InsufficientFloatException(String message) {
            super(message);
        }
    }
}
```

**FloatTransaction.java:**
```java
package com.agentbanking.float.domain.model;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;

public final class FloatTransaction {
    private final TransactionId transactionId;
    private final AgentId agentId;
    private final FloatTransactionType transactionType;
    private final Money amount;
    private final Money balanceAfter;
    private final String description;
    private final Instant timestamp;

    private FloatTransaction(TransactionId transactionId, AgentId agentId, FloatTransactionType transactionType, Money amount, Money balanceAfter, String description, Instant timestamp) {
        this.transactionId = transactionId;
        this.agentId = agentId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static FloatTransaction create(TransactionId transactionId, AgentId agentId, FloatTransactionType transactionType, Money amount, Money balanceAfter, String description) {
        return new FloatTransaction(
            transactionId, agentId, transactionType, amount, balanceAfter, description, Instant.now()
        );
    }

    public TransactionId getTransactionId() { return transactionId; }
    public AgentId getAgentId() { return agentId; }
    public FloatTransactionType getTransactionType() { return transactionType; }
    public Money getAmount() { return amount; }
    public Money getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
}
```

**FloatTransactionType.java:**
```java
package com.agentbanking.float.domain.model;

public enum FloatTransactionType {
    DEBIT,
    CREDIT,
    RESERVATION,
    RESERVATION_RELEASE
}
```

**AgentFloatRepository.java (port):**
```java
package com.agentbanking.float.domain.port.out;

import com.agentbanking.float.domain.model.AgentFloat;
import com.agentbanking.shared.identity.domain.AgentId;
import java.util.Optional;

public interface AgentFloatRepository {
    Optional<AgentFloat> findByAgentId(AgentId agentId);
    AgentFloat save(AgentFloat agentFloat);
    Optional<AgentFloat> findByAgentIdForUpdate(AgentId agentId);
}
```

**BalanceInquiryUseCase.java:**
```java
package com.agentbanking.float.domain.port.in;

import com.agentbanking.float.application.dto.BalanceInquiryResult;
import com.agentbanking.shared.identity.domain.AgentId;

public interface BalanceInquiryUseCase {
    BalanceInquiryResult getBalance(AgentId agentId);
}
```

**DebitFloatUseCase.java:**
```java
package com.agentbanking.float.domain.port.in;

import com.agentbanking.float.application.dto.FloatOperationResult;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public interface DebitFloatUseCase {
    FloatOperationResult debit(AgentId agentId, Money amount, TransactionId transactionId);
}
```

**CreditFloatUseCase.java:**
```java
package com.agentbanking.float.domain.port.in;

import com.agentbanking.float.application.dto.FloatOperationResult;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public interface CreditFloatUseCase {
    FloatOperationResult credit(AgentId agentId, Money amount, TransactionId transactionId);
}
```

**LockFloatUseCase.java (CRITICAL — called by Plans 09/10):**
```java
package com.agentbanking.float.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

/**
 * Use case for reserving agent float during saga orchestration.
 * Called by Plans 09 (Switch Adapter) and 10 (Transaction Orchestrator).
 * 
 * Returns a reservation ID string that must be passed to ReleaseFloatUseCase.
 */
public interface LockFloatUseCase {
    /**
     * Reserve float for a transaction. Returns reservation ID.
     * @param agentId the agent whose float to reserve
     * @param amount the amount to reserve
     * @return reservation ID string for later release
     */
    String reserveFloat(AgentId agentId, Money amount);
}
```

**ReleaseFloatUseCase.java (CRITICAL — called by Plans 09/10):**
```java
package com.agentbanking.float.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

/**
 * Use case for releasing a previously reserved float.
 * Called by Plans 09 (Switch Adapter) and 10 (Transaction Orchestrator).
 */
public interface ReleaseFloatUseCase {
    void releaseReservation(AgentId agentId, Money amount);
}
```

**FloatService.java:**
```java
package com.agentbanking.float.domain.service;

import com.agentbanking.float.domain.model.AgentFloat;
import com.agentbanking.float.domain.model.FloatTransaction;
import com.agentbanking.float.domain.model.FloatTransactionType;
import com.agentbanking.float.domain.port.out.AgentFloatRepository;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import java.util.UUID;

public class FloatService {
    private final AgentFloatRepository repository;

    public FloatService(AgentFloatRepository repository) {
        this.repository = repository;
    }

    public Money getBalance(AgentId agentId) {
        return repository.findByAgentId(agentId)
            .map(AgentFloat::getAvailableBalance)
            .orElse(Money.zero());
    }

    public AgentFloat getAgentFloat(AgentId agentId) {
        return repository.findByAgentId(agentId)
            .orElseThrow(() -> new AgentFloatNotFoundException("AgentFloat not found for: " + agentId));
    }

    public FloatTransaction debitFloat(AgentId agentId, Money amount, TransactionId txnId) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        AgentFloat current = repository.findByAgentIdForUpdate(agentId)
            .orElseThrow(() -> new AgentFloatNotFoundException("AgentFloat not found for: " + agentId));
        
        if (!current.hasAvailableBalance(amount)) {
            throw new InsufficientFloatException("Insufficient float for agent: " + agentId);
        }
        
        AgentFloat updated = current.debit(amount);
        repository.save(updated);
        
        return FloatTransaction.create(
            txnId, agentId, FloatTransactionType.DEBIT, amount, updated.getBalance(), "Debit float"
        );
    }

    public FloatTransaction creditFloat(AgentId agentId, Money amount, TransactionId txnId) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        AgentFloat current = repository.findByAgentIdForUpdate(agentId)
            .orElseThrow(() -> new AgentFloatNotFoundException("AgentFloat not found for: " + agentId));
        
        AgentFloat updated = current.credit(amount);
        repository.save(updated);
        
        return FloatTransaction.create(
            txnId, agentId, FloatTransactionType.CREDIT, amount, updated.getBalance(), "Credit float"
        );
    }

    /**
     * Reserve float and return a reservation ID.
     * Used by saga orchestration (Plans 09, 10).
     */
    public String reserveFloat(AgentId agentId, Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        AgentFloat current = repository.findByAgentIdForUpdate(agentId)
            .orElseThrow(() -> new AgentFloatNotFoundException("AgentFloat not found for: " + agentId));
        
        AgentFloat reserved = current.reserve(amount);
        repository.save(reserved);
        
        String reservationId = "RES-" + UUID.randomUUID().toString();
        return reservationId;
    }

    /**
     * Release a previously reserved float.
     * Used by saga orchestration (Plans 09, 10).
     */
    public void releaseReservation(AgentId agentId, Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        AgentFloat current = repository.findByAgentIdForUpdate(agentId)
            .orElseThrow(() -> new AgentFloatNotFoundException("AgentFloat not found for: " + agentId));
        
        AgentFloat released = current.releaseReservation(amount);
        repository.save(released);
    }

    public static class AgentFloatNotFoundException extends RuntimeException {
        public AgentFloatNotFoundException(String message) { super(message); }
    }

    public static class InsufficientFloatException extends RuntimeException {
        public InsufficientFloatException(String message) { super(message); }
    }
}
```

---

## Task 3: Float Bounded Context — Application DTOs

**Files to create:**
- `src/main/java/com/agentbanking/float/application/dto/BalanceInquiryResult.java`
- `src/main/java/com/agentbanking/float/application/dto/FloatOperationResult.java`

```java
package com.agentbanking.float.application.dto;

import com.agentbanking.float.domain.model.AgentFloat;
import java.math.BigDecimal;

public record BalanceInquiryResult(
    String agentId,
    BigDecimal balance,
    BigDecimal reservedBalance,
    BigDecimal availableBalance,
    String currency
) {
    public static BalanceInquiryResult from(AgentFloat agentFloat) {
        return new BalanceInquiryResult(
            agentFloat.getAgentId().value(),
            agentFloat.getBalance().amount(),
            agentFloat.getReservedBalance().amount(),
            agentFloat.getAvailableBalance().amount(),
            agentFloat.getCurrency()
        );
    }
}
```

```java
package com.agentbanking.float.application.dto;

import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;

public record FloatOperationResult(
    String transactionId,
    String agentId,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String status,
    Instant timestamp
) {
    public static FloatOperationResult success(String transactionId, String agentId, Money amount, Money balanceAfter) {
        return new FloatOperationResult(
            transactionId, agentId, amount.amount(), balanceAfter.amount(), "SUCCESS", Instant.now()
        );
    }

    public static FloatOperationResult failure(String transactionId, String agentId, String status) {
        return new FloatOperationResult(transactionId, agentId, BigDecimal.ZERO, BigDecimal.ZERO, status, Instant.now());
    }
}
```

---

## Task 4: Float Bounded Context — Infrastructure Layer

**Files to create:**
- `src/main/java/com/agentbanking/float/infrastructure/persistence/entity/AgentFloatEntity.java`
- `src/main/java/com/agentbanking/float/infrastructure/persistence/entity/FloatTransactionEntity.java`
- `src/main/java/com/agentbanking/float/infrastructure/persistence/repository/AgentFloatRepositoryImpl.java`
- `src/main/java/com/agentbanking/float/infrastructure/primary/FloatController.java`

**AgentFloatEntity.java:**
```java
package com.agentbanking.float.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "agent_float")
public class AgentFloatEntity {
    @Id
    @Column(name = "agent_id", length = 50)
    private String agentId;

    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(name = "reserved_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal reservedBalance;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public AgentFloatEntity() {}

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
```

**FloatTransactionEntity.java:**
```java
package com.agentbanking.float.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "float_transaction")
public class FloatTransactionEntity {
    @Id
    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    @Column(name = "transaction_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private FloatTransactionTypeEnum transactionType;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public enum FloatTransactionTypeEnum {
        DEBIT, CREDIT, RESERVATION, RESERVATION_RELEASE
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public FloatTransactionTypeEnum getTransactionType() { return transactionType; }
    public void setTransactionType(FloatTransactionTypeEnum transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```

**AgentFloatRepositoryImpl.java:**
```java
package com.agentbanking.float.infrastructure.persistence.repository;

import com.agentbanking.float.domain.model.AgentFloat;
import com.agentbanking.float.domain.port.out.AgentFloatRepository;
import com.agentbanking.float.infrastructure.persistence.entity.AgentFloatEntity;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public class AgentFloatRepositoryImpl implements AgentFloatRepository {

    @PersistenceContext(unitName = "float")
    private EntityManager em;

    @Override
    public Optional<AgentFloat> findByAgentId(AgentId agentId) {
        return em.createQuery(
                "SELECT f FROM AgentFloatEntity f WHERE f.agentId = :id",
                AgentFloatEntity.class)
            .setParameter("id", agentId.value())
            .getResultList()
            .stream()
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public Optional<AgentFloat> findByAgentIdForUpdate(AgentId agentId) {
        return em.createQuery(
                "SELECT f FROM AgentFloatEntity f WHERE f.agentId = :id",
                AgentFloatEntity.class)
            .setParameter("id", agentId.value())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList()
            .stream()
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public AgentFloat save(AgentFloat agentFloat) {
        AgentFloatEntity entity = findEntityByAgentId(agentFloat.getAgentId())
            .orElseGet(() -> {
                AgentFloatEntity newEntity = new AgentFloatEntity();
                newEntity.setAgentId(agentFloat.getAgentId().value());
                newEntity.setCurrency(agentFloat.getCurrency());
                newEntity.setUpdatedAt(Instant.now());
                newEntity.setBalance(agentFloat.getBalance().amount());
                newEntity.setReservedBalance(agentFloat.getReservedBalance().amount());
                return newEntity;
            });

        entity.setBalance(agentFloat.getBalance().amount());
        entity.setReservedBalance(agentFloat.getReservedBalance().amount());
        entity.setUpdatedAt(Instant.now());

        if (entity.getVersion() == null) {
            em.persist(entity);
        } else {
            em.merge(entity);
        }

        em.flush();
        return toDomain(entity);
    }

    private Optional<AgentFloatEntity> findEntityByAgentId(AgentId agentId) {
        return em.createQuery(
                "SELECT f FROM AgentFloatEntity f WHERE f.agentId = :id",
                AgentFloatEntity.class)
            .setParameter("id", agentId.value())
            .getResultList()
            .stream()
            .findFirst();
    }

    private AgentFloat toDomain(AgentFloatEntity entity) {
        return AgentFloat.restore(
            AgentId.of(entity.getAgentId()),
            Money.of(entity.getBalance()),
            Money.of(entity.getReservedBalance()),
            entity.getCurrency(),
            entity.getUpdatedAt(),
            entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }
}
```

**FloatController.java:**
```java
package com.agentbanking.float.infrastructure.primary;

import com.agentbanking.float.application.dto.BalanceInquiryResult;
import com.agentbanking.float.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.float.domain.service.FloatService;
import com.agentbanking.shared.identity.domain.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/float")
public class FloatController {
    private static final Logger log = LoggerFactory.getLogger(FloatController.class);

    private final BalanceInquiryUseCase balanceInquiryUseCase;
    private final FloatService floatService;

    public FloatController(BalanceInquiryUseCase balanceInquiryUseCase, FloatService floatService) {
        this.balanceInquiryUseCase = balanceInquiryUseCase;
        this.floatService = floatService;
    }

    @GetMapping("/{agentId}/balance")
    public ResponseEntity<BalanceInquiryResult> getBalance(@PathVariable String agentId) {
        log.info("Balance inquiry for agent: {}", agentId);
        try {
            BalanceInquiryResult result = balanceInquiryUseCase.getBalance(AgentId.of(agentId));
            return ResponseEntity.ok(result);
        } catch (FloatService.AgentFloatNotFoundException e) {
            log.warn("AgentFloat not found: {}", agentId);
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## Task 5: Float Config — Domain Service Registration (Law V)

**File:** `src/main/java/com/agentbanking/float/config/FloatDomainServiceConfig.java` (scaffolded by Seed4J — add beans)

```java
package com.agentbanking.float.config;

import com.agentbanking.float.application.dto.BalanceInquiryResult;
import com.agentbanking.float.application.dto.FloatOperationResult;
import com.agentbanking.float.domain.model.AgentFloat;
import com.agentbanking.float.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.float.domain.port.in.CreditFloatUseCase;
import com.agentbanking.float.domain.port.in.DebitFloatUseCase;
import com.agentbanking.float.domain.port.in.LockFloatUseCase;
import com.agentbanking.float.domain.port.in.ReleaseFloatUseCase;
import com.agentbanking.float.domain.port.out.AgentFloatRepository;
import com.agentbanking.float.domain.service.FloatService;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FloatDomainServiceConfig {

    @Bean
    public FloatService floatService(AgentFloatRepository repository) {
        return new FloatService(repository);
    }

    @Bean
    public BalanceInquiryUseCase balanceInquiryUseCase(FloatService floatService) {
        return agentId -> {
            AgentFloat agentFloat = floatService.getAgentFloat(agentId);
            return BalanceInquiryResult.from(agentFloat);
        };
    }

    @Bean
    public DebitFloatUseCase debitFloatUseCase(FloatService floatService) {
        return (agentId, amount, txnId) -> {
            var txn = floatService.debitFloat(agentId, amount, txnId);
            return FloatOperationResult.success(txnId.value(), agentId.value(), amount, txn.getBalanceAfter());
        };
    }

    @Bean
    public CreditFloatUseCase creditFloatUseCase(FloatService floatService) {
        return (agentId, amount, txnId) -> {
            var txn = floatService.creditFloat(agentId, amount, txnId);
            return FloatOperationResult.success(txnId.value(), agentId.value(), amount, txn.getBalanceAfter());
        };
    }

    @Bean
    public LockFloatUseCase lockFloatUseCase(FloatService floatService) {
        return floatService::reserveFloat;
    }

    @Bean
    public ReleaseFloatUseCase releaseFloatUseCase(FloatService floatService) {
        return floatService::releaseReservation;
    }
}
```

---

## Task 6: Float Tests

**File:** `src/test/java/com/agentbanking/float/domain/service/FloatServiceTest.java`

```java
package com.agentbanking.float.domain.service;

import com.agentbanking.float.domain.model.AgentFloat;
import com.agentbanking.float.domain.model.FloatTransactionType;
import com.agentbanking.float.domain.port.out.AgentFloatRepository;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FloatService")
class FloatServiceTest {

    @Mock
    private AgentFloatRepository repository;

    private FloatService service;

    private final AgentId testAgentId = AgentId.of("AGT-001");

    @BeforeEach
    void setUp() {
        service = new FloatService(repository);
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalanceTests {

        @Test
        @DisplayName("returns available balance when float exists")
        void returnsAvailableBalance() {
            AgentFloat floatWithBalance = AgentFloat.restore(
                testAgentId,
                Money.of(new BigDecimal("10000.00")),
                Money.of(new BigDecimal("500.00")),
                "MYR",
                java.time.Instant.now(),
                1L
            );
            when(repository.findByAgentId(testAgentId)).thenReturn(Optional.of(floatWithBalance));

            Money result = service.getBalance(testAgentId);

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("9500.00"));
        }

        @Test
        @DisplayName("returns zero when float not found")
        void returnsZeroWhenNotFound() {
            when(repository.findByAgentId(testAgentId)).thenReturn(Optional.empty());

            Money result = service.getBalance(testAgentId);

            assertThat(result).isEqualTo(Money.zero());
        }
    }

    @Nested
    @DisplayName("debitFloat")
    class DebitFloatTests {

        @Test
        @DisplayName("successfully debits float")
        void successfullyDebitsFloat() {
            AgentFloat currentFloat = AgentFloat.restore(
                testAgentId,
                Money.of(new BigDecimal("10000.00")),
                Money.zero(),
                "MYR",
                java.time.Instant.now(),
                1L
            );
            when(repository.findByAgentIdForUpdate(testAgentId)).thenReturn(Optional.of(currentFloat));
            when(repository.save(any(AgentFloat.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionId txnId = TransactionId.generate();
            var result = service.debitFloat(testAgentId, Money.of(new BigDecimal("500.00")), txnId);

            assertThat(result.getTransactionType()).isEqualTo(FloatTransactionType.DEBIT);
            assertThat(result.getAmount().amount()).isEqualByComparingTo(new BigDecimal("500.00"));
            verify(repository).save(any(AgentFloat.class));
        }

        @Test
        @DisplayName("throws when insufficient float")
        void throwsWhenInsufficient() {
            AgentFloat currentFloat = AgentFloat.restore(
                testAgentId,
                Money.of(new BigDecimal("200.00")),
                Money.zero(),
                "MYR",
                java.time.Instant.now(),
                1L
            );
            when(repository.findByAgentIdForUpdate(testAgentId)).thenReturn(Optional.of(currentFloat));

            assertThatThrownBy(() -> service.debitFloat(
                testAgentId,
                Money.of(new BigDecimal("500.00")),
                TransactionId.generate()
            )).isInstanceOf(FloatService.InsufficientFloatException.class);
        }
    }

    @Nested
    @DisplayName("reserveFloat")
    class ReserveFloatTests {

        @Test
        @DisplayName("successfully reserves float and returns reservation ID")
        void successfullyReservesFloat() {
            AgentFloat currentFloat = AgentFloat.restore(
                testAgentId,
                Money.of(new BigDecimal("10000.00")),
                Money.zero(),
                "MYR",
                java.time.Instant.now(),
                1L
            );
            when(repository.findByAgentIdForUpdate(testAgentId)).thenReturn(Optional.of(currentFloat));
            when(repository.save(any(AgentFloat.class))).thenAnswer(inv -> inv.getArgument(0));

            String reservationId = service.reserveFloat(testAgentId, Money.of(new BigDecimal("5000.00")));

            assertThat(reservationId).startsWith("RES-");
            verify(repository).save(any(AgentFloat.class));
        }
    }
}
```

---

## Task 7: Ledger Bounded Context — Domain Layer

**Files to create:**
- `src/main/java/com/agentbanking/ledger/domain/model/AccountType.java`
- `src/main/java/com/agentbanking/ledger/domain/model/EntryType.java`
- `src/main/java/com/agentbanking/ledger/domain/model/Account.java`
- `src/main/java/com/agentbanking/ledger/domain/model/LedgerEntry.java`
- `src/main/java/com/agentbanking/ledger/domain/port/out/LedgerRepository.java`
- `src/main/java/com/agentbanking/ledger/domain/port/out/AccountRepository.java`
- `src/main/java/com/agentbanking/ledger/domain/port/in/RecordDoubleEntryUseCase.java`
- `src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`
- `src/main/java/com/agentbanking/ledger/domain/service/DoubleEntryService.java`

```java
package com.agentbanking.ledger.domain.model;

public enum AccountType {
    CASH,
    BANK_SETTLEMENT,
    CUSTOMER,
    AGENT_FLOAT,
    COMMISSION_INCOME,
    MDR_INCOME
}
```

```java
package com.agentbanking.ledger.domain.model;

public enum EntryType {
    DEBIT,
    CREDIT
}
```

```java
package com.agentbanking.ledger.domain.model;

import com.agentbanking.shared.money.domain.Money;

public record Account(
    String accountCode,
    AccountType accountType,
    Money balance
) {
    public static Account create(String accountCode, AccountType accountType) {
        return new Account(accountCode, accountType, Money.zero());
    }
}
```

```java
package com.agentbanking.ledger.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.util.UUID;

public record LedgerEntry(
    UUID id,
    String accountCode,
    EntryType entryType,
    Money amount,
    Money balanceBefore,
    Money balanceAfter,
    String transactionId,
    String description
) {
    public static LedgerEntry create(
        String accountCode,
        EntryType entryType,
        Money amount,
        Money balanceBefore,
        Money balanceAfter,
        String transactionId,
        String description
    ) {
        return new LedgerEntry(
            UUID.randomUUID(), accountCode, entryType, amount, balanceBefore, balanceAfter, transactionId, description
        );
    }
}
```

**LedgerRepository.java (port):**
```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.LedgerEntry;
import java.util.List;

public interface LedgerRepository {
    LedgerEntry save(LedgerEntry entry);
    List<LedgerEntry> findByTransactionId(String transactionId);
}
```

**AccountRepository.java (port):**
```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.Account;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByAccountCode(String accountCode);
    Account save(Account account);
}
```

**RecordDoubleEntryUseCase.java:**
```java
package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public interface RecordDoubleEntryUseCase {
    void record(
        TransactionId transactionId,
        String debitAccountCode,
        String creditAccountCode,
        Money amount,
        String description
    );
}
```

**LedgerService.java:**
```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.Account;
import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.shared.money.domain.Money;
import java.util.List;

public class LedgerService {
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;

    public LedgerService(LedgerRepository ledgerRepository, AccountRepository accountRepository) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
    }

    public LedgerEntry recordEntry(String accountCode, EntryType entryType, Money amount, Money balanceBefore, Money balanceAfter, String transactionId, String description) {
        LedgerEntry entry = LedgerEntry.create(accountCode, entryType, amount, balanceBefore, balanceAfter, transactionId, description);
        return ledgerRepository.save(entry);
    }

    public List<LedgerEntry> getEntriesByTransactionId(String transactionId) {
        return ledgerRepository.findByTransactionId(transactionId);
    }

    public Account getOrCreateAccount(String accountCode, AccountType accountType) {
        return accountRepository.findByAccountCode(accountCode)
            .orElseGet(() -> Account.create(accountCode, accountType));
    }
}
```

**DoubleEntryService.java:**
```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.in.RecordDoubleEntryUseCase;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;

public class DoubleEntryService implements RecordDoubleEntryUseCase {
    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;

    public DoubleEntryService(LedgerService ledgerService, AccountRepository accountRepository) {
        this.ledgerService = ledgerService;
        this.accountRepository = accountRepository;
    }

    @Override
    public void record(TransactionId transactionId, String debitAccountCode, String creditAccountCode, Money amount, String description) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Money debitBalanceBefore = accountRepository.findByAccountCode(debitAccountCode)
            .map(Account::balance)
            .orElse(Money.zero());
        Money creditBalanceBefore = accountRepository.findByAccountCode(creditAccountCode)
            .map(Account::balance)
            .orElse(Money.zero());

        Money debitBalanceAfter = debitBalanceBefore.add(amount);
        Money creditBalanceAfter = creditBalanceBefore.add(amount);

        LedgerEntry debitEntry = LedgerEntry.create(
            debitAccountCode, EntryType.DEBIT, amount, debitBalanceBefore, debitBalanceAfter,
            transactionId.value(), description
        );

        LedgerEntry creditEntry = LedgerEntry.create(
            creditAccountCode, EntryType.CREDIT, amount, creditBalanceBefore, creditBalanceAfter,
            transactionId.value(), description
        );

        ledgerService.recordEntry(
            debitEntry.accountCode(), debitEntry.entryType(), debitEntry.amount(),
            debitEntry.balanceBefore(), debitEntry.balanceAfter(),
            debitEntry.transactionId(), debitEntry.description()
        );

        ledgerService.recordEntry(
            creditEntry.accountCode(), creditEntry.entryType(), creditEntry.amount(),
            creditEntry.balanceBefore(), creditEntry.balanceAfter(),
            creditEntry.transactionId(), creditEntry.description()
        );
    }

    public void recordCashWithdrawal(String transactionId, Money amount) {
        record(
            TransactionId.of(transactionId),
            "AGENT_FLOAT",
            "BANK_SETTLEMENT",
            amount,
            "Cash withdrawal"
        );
    }

    public void recordCashDeposit(String transactionId, Money amount) {
        record(
            TransactionId.of(transactionId),
            "BANK_SETTLEMENT",
            "CUSTOMER",
            amount,
            "Cash deposit"
        );
    }
}
```

---

## Task 8: Ledger Bounded Context — Infrastructure Layer

**Files to create:**
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/AccountEntity.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/LedgerEntryEntity.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/LedgerRepositoryImpl.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/AccountRepositoryImpl.java`
- `src/main/java/com/agentbanking/ledger/infrastructure/primary/LedgerController.java`

**AccountEntity.java:**
```java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_account")
public class AccountEntity {
    @Id
    @Column(name = "account_code", length = 50)
    private String accountCode;

    @Column(name = "account_type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountTypeEnum accountType;

    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;

    public enum AccountTypeEnum {
        CASH, BANK_SETTLEMENT, CUSTOMER, AGENT_FLOAT, COMMISSION_INCOME, MDR_INCOME
    }

    public AccountEntity() {}

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public AccountTypeEnum getAccountType() { return accountType; }
    public void setAccountType(AccountTypeEnum accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
```

**LedgerEntryEntity.java:**
```java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntryEntity {
    @Id
    private UUID id;

    @Column(name = "account_code", length = 50, nullable = false)
    private String accountCode;

    @Column(name = "entry_type", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private EntryTypeEnum entryType;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_id", length = 50, nullable = false)
    private String transactionId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum EntryTypeEnum {
        DEBIT, CREDIT
    }

    public LedgerEntryEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public EntryTypeEnum getEntryType() { return entryType; }
    public void setEntryType(EntryTypeEnum entryType) { this.entryType = entryType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

**LedgerRepositoryImpl.java:**
```java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.LedgerEntryEntity;
import com.agentbanking.shared.money.domain.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public class LedgerRepositoryImpl implements LedgerRepository {

    @PersistenceContext(unitName = "ledger")
    private EntityManager em;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryEntity entity = toEntity(entry);
        entity.setCreatedAt(Instant.now());
        em.persist(entity);
        em.flush();
        return toDomain(entity);
    }

    @Override
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        return em.createQuery(
                "SELECT e FROM LedgerEntryEntity e WHERE e.transactionId = :txnId",
                LedgerEntryEntity.class)
            .setParameter("txnId", transactionId)
            .getResultList()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private LedgerEntryEntity toEntity(LedgerEntry entry) {
        LedgerEntryEntity entity = new LedgerEntryEntity();
        entity.setId(entry.id());
        entity.setAccountCode(entry.accountCode());
        entity.setEntryType(LedgerEntryEntity.EntryTypeEnum.valueOf(entry.entryType().name()));
        entity.setAmount(entry.amount().amount());
        entity.setBalanceBefore(entry.balanceBefore().amount());
        entity.setBalanceAfter(entry.balanceAfter().amount());
        entity.setTransactionId(entry.transactionId());
        entity.setDescription(entry.description());
        return entity;
    }

    private LedgerEntry toDomain(LedgerEntryEntity entity) {
        return LedgerEntry.create(
            entity.getAccountCode(),
            EntryType.valueOf(entity.getEntryType().name()),
            Money.of(entity.getAmount()),
            Money.of(entity.getBalanceBefore()),
            Money.of(entity.getBalanceAfter()),
            entity.getTransactionId(),
            entity.getDescription()
        );
    }
}
```

**AccountRepositoryImpl.java:**
```java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.Account;
import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.AccountEntity;
import com.agentbanking.shared.money.domain.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class AccountRepositoryImpl implements AccountRepository {

    @PersistenceContext(unitName = "ledger")
    private EntityManager em;

    @Override
    public Optional<Account> findByAccountCode(String accountCode) {
        return em.createQuery(
                "SELECT a FROM AccountEntity a WHERE a.accountCode = :code",
                AccountEntity.class)
            .setParameter("code", accountCode)
            .getResultList()
            .stream()
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = findEntityByAccountCode(account.accountCode())
            .orElseGet(() -> {
                AccountEntity newEntity = new AccountEntity();
                newEntity.setAccountCode(account.accountCode());
                return newEntity;
            });

        entity.setAccountType(AccountEntity.AccountTypeEnum.valueOf(account.accountType().name()));
        entity.setBalance(account.balance().amount());

        if (entity.getAccountType() == null) {
            em.persist(entity);
        } else {
            em.merge(entity);
        }

        em.flush();
        return toDomain(entity);
    }

    private Optional<AccountEntity> findEntityByAccountCode(String accountCode) {
        return em.createQuery(
                "SELECT a FROM AccountEntity a WHERE a.accountCode = :code",
                AccountEntity.class)
            .setParameter("code", accountCode)
            .getResultList()
            .stream()
            .findFirst();
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
            entity.getAccountCode(),
            AccountType.valueOf(entity.getAccountType().name()),
            Money.of(entity.getBalance())
        );
    }
}
```

**LedgerController.java:**
```java
package com.agentbanking.ledger.infrastructure.primary;

import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {
    private static final Logger log = LoggerFactory.getLogger(LedgerController.class);

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/transactions/{transactionId}/entries")
    public ResponseEntity<List<LedgerEntry>> getEntries(@PathVariable String transactionId) {
        log.info("Get ledger entries for transaction: {}", transactionId);
        List<LedgerEntry> entries = ledgerService.getEntriesByTransactionId(transactionId);
        return ResponseEntity.ok(entries);
    }
}
```

---

## Task 9: Ledger Config — Domain Service Registration (Law V)

**File:** `src/main/java/com/agentbanking/ledger/config/LedgerDomainServiceConfig.java` (scaffolded by Seed4J — add beans)

```java
package com.agentbanking.ledger.config;

import com.agentbanking.ledger.domain.port.in.RecordDoubleEntryUseCase;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.ledger.domain.service.DoubleEntryService;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerDomainServiceConfig {

    @Bean
    public LedgerService ledgerService(LedgerRepository ledgerRepository, AccountRepository accountRepository) {
        return new LedgerService(ledgerRepository, accountRepository);
    }

    @Bean
    public DoubleEntryService doubleEntryService(LedgerService ledgerService, AccountRepository accountRepository) {
        return new DoubleEntryService(ledgerService, accountRepository);
    }

    @Bean
    public RecordDoubleEntryUseCase recordDoubleEntryUseCase(DoubleEntryService doubleEntryService) {
        return doubleEntryService;
    }
}
```

---

## Task 10: Ledger Tests

**File:** `src/test/java/com/agentbanking/ledger/domain/service/LedgerServiceTest.java`

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.Account;
import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.shared.money.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerService")
class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private AccountRepository accountRepository;

    private LedgerService service;

    @BeforeEach
    void setUp() {
        service = new LedgerService(ledgerRepository, accountRepository);
    }

    @Nested
    @DisplayName("recordEntry")
    class RecordEntryTests {

        @Test
        @DisplayName("creates ledger entry")
        void createsLedgerEntry() {
            Money amount = Money.of(new BigDecimal("500.00"));
            Money balanceBefore = Money.zero();
            Money balanceAfter = Money.of(new BigDecimal("500.00"));
            String txnId = "TXN-001";

            when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            LedgerEntry result = service.recordEntry(
                "AGT_FLOAT", EntryType.DEBIT, amount, balanceBefore, balanceAfter, txnId, "Test entry"
            );

            assertThat(result.accountCode()).isEqualTo("AGT_FLOAT");
            assertThat(result.entryType()).isEqualTo(EntryType.DEBIT);
            assertThat(result.amount()).isEqualTo(amount);
            verify(ledgerRepository).save(any(LedgerEntry.class));
        }
    }

    @Nested
    @DisplayName("getEntriesByTransactionId")
    class GetEntriesTests {

        @Test
        @DisplayName("retrieves entries by transaction ID")
        void retrievesEntriesByTransactionId() {
            String txnId = "TXN-002";
            LedgerEntry entry = LedgerEntry.create(
                "BANK_SETTLEMENT", EntryType.CREDIT,
                Money.of(new BigDecimal("100.00")), Money.zero(), Money.of(new BigDecimal("100.00")),
                txnId, "Test"
            );
            when(ledgerRepository.findByTransactionId(txnId)).thenReturn(List.of(entry));

            List<LedgerEntry> result = service.getEntriesByTransactionId(txnId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).transactionId()).isEqualTo(txnId);
        }
    }
}
```

**File:** `src/test/java/com/agentbanking/ledger/domain/service/DoubleEntryServiceTest.java`

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.Account;
import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.port.out.AccountRepository;
import com.agentbanking.shared.identity.domain.TransactionId;
import com.agentbanking.shared.money.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoubleEntryService")
class DoubleEntryServiceTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AccountRepository accountRepository;

    private DoubleEntryService service;

    @BeforeEach
    void setUp() {
        service = new DoubleEntryService(ledgerService, accountRepository);
    }

    @Nested
    @DisplayName("record")
    class RecordTests {

        @Test
        @DisplayName("creates debit and credit entries")
        void createsDebitAndCreditEntries() {
            TransactionId txnId = TransactionId.generate();
            Money amount = Money.of(new BigDecimal("500.00"));

            when(accountRepository.findByAccountCode(any())).thenReturn(Optional.of(
                Account.create("AGT_FLOAT", AccountType.AGENT_FLOAT)
            ));

            service.record(txnId, "AGT_FLOAT", "BANK_SETTLEMENT", amount, "Test double entry");

            verify(ledgerService, times(2)).recordEntry(
                any(), any(), any(), any(), any(), any(), any()
            );
        }

        @Test
        @DisplayName("throws on negative amount")
        void throwsOnNegativeAmount() {
            assertThatThrownBy(() -> service.record(
                TransactionId.generate(),
                "AGT_FLOAT",
                "BANK_SETTLEMENT",
                Money.of(new BigDecimal("-100.00")),
                "Test"
            )).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Amount must be positive");
        }
    }
}
```

---

## Verification

```bash
./gradlew test -Dtest.include="**/float/**/*Test*,**/ledger/**/*Test*"
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files Created | Tests |
|------|-----------|---------------|-------|
| 1 | Shared Domain Model | 3 (if not present) | 0 |
| 2 | Float Domain Layer | 10 | 0 |
| 3 | Float Application DTOs | 2 | 0 |
| 4 | Float Infrastructure | 4 | 0 |
| 5 | Float Config (Law V) | 0 (modify scaffolded) | 0 |
| 6 | Float Tests | 1 | 1 |
| 7 | Ledger Domain Layer | 9 | 0 |
| 8 | Ledger Infrastructure | 5 | 0 |
| 9 | Ledger Config (Law V) | 0 (modify scaffolded) | 0 |
| 10 | Ledger Tests | 2 | 2 |
| **Total** | | **36** | **3** |

**Cross-Plan Interfaces (CRITICAL for Plans 09/10):**

| Interface | Method | Signature |
|-----------|--------|-----------|
| `LockFloatUseCase` | `reserveFloat` | `(AgentId, Money) → String` (reservation ID) |
| `ReleaseFloatUseCase` | `releaseReservation` | `(String reservationId, AgentId, Money) → void` |

All components follow hexagonal architecture:
- Domain layer has ZERO Spring/JPA imports
- Domain services registered via `@Bean` in config (Law V)
- Infrastructure adapters use `@Repository`, `@RestController` (Law VI)
- PESSIMISTIC_WRITE locks on float operations
