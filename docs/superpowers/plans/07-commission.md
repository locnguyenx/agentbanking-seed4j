# Plan 07: Commission Service

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Commission bounded context with commission calculation and settlement services per design spec Section 6.5.

**Architecture:** Hexagonal architecture with domain services as Spring beans via `@Bean` (Law V). Commission entries created for every successful transaction, settlement at EOD.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Gradle

**Wave:** 2 (Normal Service — no Temporal)

**Approach:** Seed4J-first. Use Seed4J CLI to scaffold package structure, config classes, Flyway stubs, ArchUnit tests, and test templates. Write custom business logic manually.

**Existing stubs to replace:**
- `src/main/java/com/agentbanking/commission/domain/model/Commission.java`
- `src/main/java/com/agentbanking/commission/domain/service/CommissionService.java`
- `src/main/java/com/agentbanking/commission/domain/port/in/CalculateCommissionUseCase.java`
- `src/main/java/com/agentbanking/commission/infrastructure/primary/CommissionController.java`
- `src/main/java/com/agentbanking/commission/infrastructure/secondary/CommissionRepositoryImpl.java`

---

## Seed4J Scaffolding

Run these Seed4J commands to scaffold the commission bounded context:

```bash
# Apply hexagonal architecture template to commission context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal --context commission

# This creates:
# - src/main/java/com/agentbanking/commission/config/CommissionDomainServiceConfig.java
# - src/main/java/com/agentbanking/commission/config/CommissionDatabaseConfig.java
# - Flyway stub: db/migration/commission/
# - ArchUnit test stub
# - Test templates
```

After scaffolding, verify:
- `src/main/java/com/agentbanking/commission/config/CommissionDomainServiceConfig.java` exists
- `src/main/java/com/agentbanking/commission/config/CommissionDatabaseConfig.java` exists
- `src/test/java/com/agentbanking/commission/` has test stubs

---

## Task 1: Domain Model — Enums and CommissionEntry

**Files to create/replace:**
- `src/main/java/com/agentbanking/commission/domain/model/CommissionType.java`
- `src/main/java/com/agentbanking/commission/domain/model/CommissionStatus.java`
- `src/main/java/com/agentbanking/commission/domain/model/CommissionEntry.java` (replace Commission.java stub)

```java
package com.agentbanking.commission.domain.model;

public enum CommissionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BILL_PAYMENT,
    RETAIL_SALE,
    PREPAID_TOPUP
}
```

```java
package com.agentbanking.commission.domain.model;

public enum CommissionStatus {
    PENDING,
    CALCULATED,
    SETTLED,
    PAID
}
```

```java
package com.agentbanking.commission.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionEntry(
    UUID id,
    String transactionId,
    String agentId,
    CommissionType type,
    BigDecimal transactionAmount,
    BigDecimal commissionAmount,
    BigDecimal rateApplied,
    CommissionStatus status,
    Instant settledAt,
    Instant createdAt
) {
    public static CommissionEntry calculate(
        String transactionId,
        String agentId,
        CommissionType type,
        BigDecimal transactionAmount,
        BigDecimal commissionAmount,
        BigDecimal rateApplied
    ) {
        return new CommissionEntry(
            UUID.randomUUID(),
            transactionId, agentId, type, transactionAmount,
            commissionAmount, rateApplied,
            CommissionStatus.PENDING, null, Instant.now()
        );
    }

    public CommissionEntry withStatus(CommissionStatus newStatus) {
        return new CommissionEntry(
            id, transactionId, agentId, type, transactionAmount,
            commissionAmount, rateApplied, newStatus,
            newStatus == CommissionStatus.SETTLED ? Instant.now() : settledAt,
            createdAt
        );
    }
}
```

---

## Task 2: Repository Port and Use Cases

**Files to create/replace:**
- `src/main/java/com/agentbanking/commission/domain/port/out/CommissionRepository.java`
- `src/main/java/com/agentbanking/commission/domain/port/in/CalculateCommissionUseCase.java`
- `src/main/java/com/agentbanking/commission/domain/port/in/SettleCommissionUseCase.java`

```java
package com.agentbanking.commission.domain.port.out;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommissionRepository {
    CommissionEntry save(CommissionEntry entry);
    Optional<CommissionEntry> findById(UUID id);
    List<CommissionEntry> findByAgentIdAndStatus(String agentId, CommissionStatus status);
    List<CommissionEntry> findByCreatedAtBetween(Instant from, Instant to);
    List<CommissionEntry> findByAgentIdAndCreatedAtBetween(String agentId, Instant from, Instant to);
}
```

```java
package com.agentbanking.commission.domain.port.in;

import com.agentbanking.commission.domain.model.CommissionEntry;
import java.math.BigDecimal;

public interface CalculateCommissionUseCase {
    CommissionEntry calculate(
        String transactionId,
        String agentId,
        String transactionType,
        BigDecimal amount
    );
}
```

```java
package com.agentbanking.commission.domain.port.in;

import com.agentbanking.commission.domain.model.CommissionEntry;
import java.time.LocalDate;
import java.util.List;

public interface SettleCommissionUseCase {
    void settleDailyCommissions(LocalDate businessDate);
    List<CommissionEntry> getPendingSettlements(String agentId);
}
```

---

## Task 3: Domain Services

**Files to create/replace:**
- `src/main/java/com/agentbanking/commission/domain/service/CommissionCalculationService.java`
- `src/main/java/com/agentbanking/commission/domain/service/CommissionSettlementService.java`

```java
package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommissionCalculationService {

    private static final BigDecimal TIER_1_WITHDRAWAL_RATE = new BigDecimal("0.005");
    private static final BigDecimal TIER_2_WITHDRAWAL_RATE = new BigDecimal("0.007");
    private static final BigDecimal TIER_3_WITHDRAWAL_RATE = new BigDecimal("0.010");
    private static final BigDecimal TIER_1_DEPOSIT_RATE = new BigDecimal("0.003");
    private static final BigDecimal TIER_2_DEPOSIT_RATE = new BigDecimal("0.005");
    private static final BigDecimal TIER_3_DEPOSIT_RATE = new BigDecimal("0.007");
    private static final BigDecimal BILL_PAYMENT_RATE = new BigDecimal("0.002");
    private static final BigDecimal RETAIL_SALE_RATE = new BigDecimal("0.015");
    private static final BigDecimal PREPAID_TOPUP_RATE = new BigDecimal("0.005");

    private final CommissionRepository repository;

    public CommissionCalculationService(CommissionRepository repository) {
        this.repository = repository;
    }

    public CommissionEntry calculate(
        String transactionId,
        String agentId,
        String transactionType,
        BigDecimal amount
    ) {
        CommissionType commissionType = mapToCommissionType(transactionType);
        BigDecimal rate = getRateForType(commissionType, agentId);
        BigDecimal commissionAmount = amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);

        CommissionEntry entry = CommissionEntry.calculate(
            transactionId, agentId, commissionType, amount, commissionAmount, rate
        );

        return repository.save(entry);
    }

    private CommissionType mapToCommissionType(String transactionType) {
        return switch (transactionType.toUpperCase()) {
            case "CASH_WITHDRAWAL", "WITHDRAWAL" -> CommissionType.CASH_WITHDRAWAL;
            case "CASH_DEPOSIT", "DEPOSIT" -> CommissionType.CASH_DEPOSIT;
            case "BILL_PAYMENT" -> CommissionType.BILL_PAYMENT;
            case "RETAIL_SALE" -> CommissionType.RETAIL_SALE;
            case "PREPAID_TOPUP" -> CommissionType.PREPAID_TOPUP;
            default -> throw new IllegalArgumentException("Unknown transaction type: " + transactionType);
        };
    }

    private BigDecimal getRateForType(CommissionType type, String agentId) {
        return switch (type) {
            case CASH_WITHDRAWAL -> determineWithdrawalRate(agentId);
            case CASH_DEPOSIT -> determineDepositRate(agentId);
            case BILL_PAYMENT -> BILL_PAYMENT_RATE;
            case RETAIL_SALE -> RETAIL_SALE_RATE;
            case PREPAID_TOPUP -> PREPAID_TOPUP_RATE;
        };
    }

    private BigDecimal determineWithdrawalRate(String agentId) {
        String tier = extractTierFromAgentId(agentId);
        return switch (tier) {
            case "1" -> TIER_1_WITHDRAWAL_RATE;
            case "2" -> TIER_2_WITHDRAWAL_RATE;
            default -> TIER_3_WITHDRAWAL_RATE;
        };
    }

    private BigDecimal determineDepositRate(String agentId) {
        String tier = extractTierFromAgentId(agentId);
        return switch (tier) {
            case "1" -> TIER_1_DEPOSIT_RATE;
            case "2" -> TIER_2_DEPOSIT_RATE;
            default -> TIER_3_DEPOSIT_RATE;
        };
    }

    private String extractTierFromAgentId(String agentId) {
        if (agentId == null || agentId.isEmpty()) {
            return "3";
        }
        if (agentId.contains("TIER1")) return "1";
        if (agentId.contains("TIER2")) return "2";
        return "3";
    }
}
```

```java
package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class CommissionSettlementService {

    private final CommissionRepository repository;

    public CommissionSettlementService(CommissionRepository repository) {
        this.repository = repository;
    }

    public void settleDailyCommissions(LocalDate businessDate) {
        Instant startOfDay = businessDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = businessDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<CommissionEntry> pendingEntries = repository.findByCreatedAtBetween(startOfDay, endOfDay);

        for (CommissionEntry entry : pendingEntries) {
            if (entry.status() == CommissionStatus.PENDING || entry.status() == CommissionStatus.CALCULATED) {
                CommissionEntry settled = entry.withStatus(CommissionStatus.SETTLED);
                repository.save(settled);
            }
        }
    }

    public List<CommissionEntry> getPendingSettlements(String agentId) {
        return repository.findByAgentIdAndStatus(agentId, CommissionStatus.PENDING);
    }
}
```

---

## Task 4: JPA Entities

**Files to create/replace:**
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/entity/CommissionEntryEntity.java`
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/entity/CommissionRateEntity.java`

```java
package com.agentbanking.commission.infrastructure.persistence.entity;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commission_entry")
public class CommissionEntryEntity {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false)
    private CommissionType commissionType;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "commission_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal commissionAmount;

    @Column(name = "rate_applied", nullable = false, precision = 19, scale = 4)
    private BigDecimal rateApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus status;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CommissionEntryEntity() {}

    public static CommissionEntryEntity fromDomain(CommissionEntry entry) {
        CommissionEntryEntity entity = new CommissionEntryEntity();
        entity.id = entry.id();
        entity.transactionId = entry.transactionId();
        entity.agentId = entry.agentId();
        entity.commissionType = entry.type();
        entity.transactionAmount = entry.transactionAmount();
        entity.commissionAmount = entry.commissionAmount();
        entity.rateApplied = entry.rateApplied();
        entity.status = entry.status();
        entity.settledAt = entry.settledAt();
        entity.createdAt = entry.createdAt();
        return entity;
    }

    public CommissionEntry toDomain() {
        return new CommissionEntry(
            id, transactionId, agentId, commissionType, transactionAmount,
            commissionAmount, rateApplied, status, settledAt, createdAt
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public CommissionType getCommissionType() { return commissionType; }
    public void setCommissionType(CommissionType commissionType) { this.commissionType = commissionType; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getRateApplied() { return rateApplied; }
    public void setRateApplied(BigDecimal rateApplied) { this.rateApplied = rateApplied; }
    public CommissionStatus getStatus() { return status; }
    public void setStatus(CommissionStatus status) { this.status = status; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.agentbanking.commission.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commission_rate")
public class CommissionRateEntity {

    @Id
    private UUID id;

    @Column(name = "transaction_type", nullable = false, unique = true)
    private String transactionType;

    @Column(name = "rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public CommissionRateEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant effectiveTo) { this.effectiveTo = effectiveTo; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
```

---

## Task 5: Repository Implementation

**File to create/replace:**
- `src/main/java/com/agentbanking/commission/infrastructure/persistence/repository/CommissionRepositoryImpl.java`

```java
package com.agentbanking.commission.infrastructure.persistence.repository;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.commission.infrastructure.persistence.entity.CommissionEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionJpaRepository extends JpaRepository<CommissionEntryEntity, UUID> {
    List<CommissionEntryEntity> findByAgentIdAndStatus(String agentId, CommissionStatus status);
    List<CommissionEntryEntity> findByCreatedAtBetween(Instant from, Instant to);
    List<CommissionEntryEntity> findByAgentIdAndCreatedAtBetween(String agentId, Instant from, Instant to);
}

public class CommissionRepositoryImpl implements CommissionRepository {

    private final CommissionJpaRepository jpaRepository;

    public CommissionRepositoryImpl(CommissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CommissionEntry save(CommissionEntry entry) {
        CommissionEntryEntity entity = CommissionEntryEntity.fromDomain(entry);
        CommissionEntryEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<CommissionEntry> findById(UUID id) {
        return jpaRepository.findById(id).map(CommissionEntryEntity::toDomain);
    }

    @Override
    public List<CommissionEntry> findByAgentIdAndStatus(String agentId, CommissionStatus status) {
        return jpaRepository.findByAgentIdAndStatus(agentId, status)
            .stream()
            .map(CommissionEntryEntity::toDomain)
            .toList();
    }

    @Override
    public List<CommissionEntry> findByCreatedAtBetween(Instant from, Instant to) {
        return jpaRepository.findByCreatedAtBetween(from, to)
            .stream()
            .map(CommissionEntryEntity::toDomain)
            .toList();
    }

    @Override
    public List<CommissionEntry> findByAgentIdAndCreatedAtBetween(String agentId, Instant from, Instant to) {
        return jpaRepository.findByAgentIdAndCreatedAtBetween(agentId, from, to)
            .stream()
            .map(CommissionEntryEntity::toDomain)
            .toList();
    }
}
```

---

## Task 6: Controller and DTOs

**File to create:**
- `src/main/java/com/agentbanking/commission/application/dto/CommissionEntryDto.java`

**File to replace:**
- `src/main/java/com/agentbanking/commission/infrastructure/primary/CommissionController.java`

```java
package com.agentbanking.commission.application.dto;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionEntryDto(
    UUID id,
    String transactionId,
    String agentId,
    CommissionType type,
    BigDecimal transactionAmount,
    BigDecimal commissionAmount,
    BigDecimal rateApplied,
    CommissionStatus status,
    Instant settledAt,
    Instant createdAt
) {
    public static CommissionEntryDto fromDomain(CommissionEntry entry) {
        return new CommissionEntryDto(
            entry.id(), entry.transactionId(), entry.agentId(), entry.type(),
            entry.transactionAmount(), entry.commissionAmount(), entry.rateApplied(),
            entry.status(), entry.settledAt(), entry.createdAt()
        );
    }
}
```

```java
package com.agentbanking.commission.infrastructure.primary;

import com.agentbanking.commission.application.dto.CommissionEntryDto;
import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.commission.domain.service.CommissionSettlementService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commission")
public class CommissionController {

    private final CalculateCommissionUseCase calculateUseCase;
    private final CommissionSettlementService settlementService;

    public CommissionController(
            CalculateCommissionUseCase calculateUseCase,
            CommissionSettlementService settlementService) {
        this.calculateUseCase = calculateUseCase;
        this.settlementService = settlementService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<CommissionEntryDto> calculate(
            @RequestParam String transactionId,
            @RequestParam String agentId,
            @RequestParam String transactionType,
            @RequestParam BigDecimal amount) {
        
        CommissionEntry entry = calculateUseCase.calculate(transactionId, agentId, transactionType, amount);
        return ResponseEntity.ok(CommissionEntryDto.fromDomain(entry));
    }

    @GetMapping("/{agentId}/pending")
    public ResponseEntity<List<CommissionEntryDto>> getPending(@PathVariable String agentId) {
        List<CommissionEntry> pending = settlementService.getPendingSettlements(agentId);
        List<CommissionEntryDto> dtos = pending.stream()
            .map(CommissionEntryDto::fromDomain)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/settle")
    public ResponseEntity<Void> settle(@RequestParam String businessDate) {
        settlementService.settleDailyCommissions(java.time.LocalDate.parse(businessDate));
        return ResponseEntity.ok().build();
    }
}
```

---

## Task 7: Configuration

**File to replace:**
- `src/main/java/com/agentbanking/commission/config/CommissionDatabaseConfig.java`

**File to create:**
- `src/main/java/com/agentbanking/commission/config/CommissionDomainServiceConfig.java`

```java
package com.agentbanking.commission.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class CommissionDatabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.commission")
    public DataSource commissionDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

```java
package com.agentbanking.commission.config;

import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.commission.domain.port.in.SettleCommissionUseCase;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.commission.domain.service.CommissionCalculationService;
import com.agentbanking.commission.domain.service.CommissionSettlementService;
import com.agentbanking.commission.infrastructure.persistence.repository.CommissionJpaRepository;
import com.agentbanking.commission.infrastructure.persistence.repository.CommissionRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommissionDomainServiceConfig {

    @Bean
    public CommissionRepositoryImpl commissionRepositoryImpl(CommissionJpaRepository jpaRepository) {
        return new CommissionRepositoryImpl(jpaRepository);
    }

    @Bean
    public CommissionCalculationService commissionCalculationService(CommissionRepository repository) {
        return new CommissionCalculationService(repository);
    }

    @Bean
    public CommissionSettlementService commissionSettlementService(CommissionRepository repository) {
        return new CommissionSettlementService(repository);
    }

    @Bean
    public CalculateCommissionUseCase calculateCommissionUseCase(CommissionCalculationService calculationService) {
        return calculationService::calculate;
    }

    @Bean
    public SettleCommissionUseCase settleCommissionUseCase(CommissionSettlementService settlementService) {
        return settlementService;
    }
}
```

**FIX:** `SettleCommissionUseCase` is now registered as a `@Bean` in `CommissionDomainServiceConfig.java`.

---

## Task 8: Unit Tests

**File:** `src/test/java/com/agentbanking/commission/domain/service/CommissionCalculationServiceTest.java`

```java
package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionCalculationServiceTest {

    @Mock
    private CommissionRepository repository;

    private CommissionCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CommissionCalculationService(repository);
    }

    @Test
    void calculateCashWithdrawalTier1_shouldReturnCorrectCommission() {
        String transactionId = UUID.randomUUID().toString();
        String agentId = "AGENT_TIER1_001";
        BigDecimal amount = new BigDecimal("1000.00");

        when(repository.save(any(CommissionEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionEntry result = service.calculate(transactionId, agentId, "CASH_WITHDRAWAL", amount);

        assertNotNull(result);
        assertEquals(transactionId, result.transactionId());
        assertEquals(agentId, result.agentId());
        assertEquals(CommissionType.CASH_WITHDRAWAL, result.type());
        assertEquals(new BigDecimal("5.0000"), result.commissionAmount());
        assertEquals(CommissionStatus.PENDING, result.status());
    }

    @Test
    void calculateCashWithdrawalTier2_shouldReturnCorrectCommission() {
        String transactionId = UUID.randomUUID().toString();
        String agentId = "AGENT_TIER2_001";
        BigDecimal amount = new BigDecimal("1000.00");

        when(repository.save(any(CommissionEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionEntry result = service.calculate(transactionId, agentId, "CASH_WITHDRAWAL", amount);

        assertEquals(new BigDecimal("7.0000"), result.commissionAmount());
    }

    @Test
    void calculateBillPayment_shouldReturnCorrectCommission() {
        String transactionId = UUID.randomUUID().toString();
        String agentId = "AGENT_001";
        BigDecimal amount = new BigDecimal("100.00");

        when(repository.save(any(CommissionEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionEntry result = service.calculate(transactionId, agentId, "BILL_PAYMENT", amount);

        assertEquals(CommissionType.BILL_PAYMENT, result.type());
        assertEquals(new BigDecimal("0.2000"), result.commissionAmount());
    }

    @Test
    void calculateRetailSale_shouldReturnCorrectCommission() {
        String transactionId = UUID.randomUUID().toString();
        String agentId = "AGENT_001";
        BigDecimal amount = new BigDecimal("100.00");

        when(repository.save(any(CommissionEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionEntry result = service.calculate(transactionId, agentId, "RETAIL_SALE", amount);

        assertEquals(CommissionType.RETAIL_SALE, result.type());
        assertEquals(new BigDecimal("1.5000"), result.commissionAmount());
    }

    @Test
    void calculateUnknownType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.calculate("txn", "agent", "UNKNOWN_TYPE", new BigDecimal("100"))
        );
    }
}
```

**File:** `src/test/java/com/agentbanking/commission/domain/service/CommissionSettlementServiceTest.java`

```java
package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionSettlementServiceTest {

    @Mock
    private CommissionRepository repository;

    private CommissionSettlementService service;

    @BeforeEach
    void setUp() {
        service = new CommissionSettlementService(repository);
    }

    @Test
    void settleDailyCommissions_shouldSettleAllPendingEntries() {
        LocalDate businessDate = LocalDate.of(2026, 4, 6);
        Instant startOfDay = businessDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endOfDay = businessDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        CommissionEntry entry1 = createTestEntry("txn1", "agent1", CommissionStatus.PENDING);
        CommissionEntry entry2 = createTestEntry("txn2", "agent1", CommissionStatus.CALCULATED);
        CommissionEntry entry3 = createTestEntry("txn3", "agent2", CommissionStatus.SETTLED);

        when(repository.findByCreatedAtBetween(startOfDay, endOfDay))
            .thenReturn(List.of(entry1, entry2, entry3));
        when(repository.save(any(CommissionEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.settleDailyCommissions(businessDate);

        ArgumentCaptor<CommissionEntry> captor = ArgumentCaptor.forClass(CommissionEntry.class);
        verify(repository, times(2)).save(captor.capture());

        List<CommissionEntry> saved = captor.getAllValues();
        assertTrue(saved.stream().allMatch(e -> e.status() == CommissionStatus.SETTLED));
    }

    @Test
    void getPendingSettlements_shouldReturnPendingForAgent() {
        String agentId = "AGENT_001";
        CommissionEntry entry = createTestEntry("txn1", agentId, CommissionStatus.PENDING);

        when(repository.findByAgentIdAndStatus(agentId, CommissionStatus.PENDING))
            .thenReturn(List.of(entry));

        List<CommissionEntry> result = service.getPendingSettlements(agentId);

        assertEquals(1, result.size());
        assertEquals(agentId, result.get(0).agentId());
    }

    @Test
    void settleDailyCommissions_noEntries_shouldNotCallSave() {
        LocalDate businessDate = LocalDate.of(2026, 4, 6);
        Instant startOfDay = businessDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endOfDay = businessDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        when(repository.findByCreatedAtBetween(startOfDay, endOfDay))
            .thenReturn(List.of());

        service.settleDailyCommissions(businessDate);

        verify(repository, never()).save(any());
    }

    private CommissionEntry createTestEntry(String txnId, String agentId, CommissionStatus status) {
        return new CommissionEntry(
            UUID.randomUUID(), txnId, agentId, CommissionType.CASH_WITHDRAWAL,
            new BigDecimal("1000.00"), new BigDecimal("5.00"), new BigDecimal("0.005"),
            status, null, Instant.now()
        );
    }
}
```

---

## Verification

```bash
./gradlew test -Dtest.include="**/commission/**/*Test*"
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files |
|------|-----------|-------|
| 1 | Domain Model (Enums + CommissionEntry) | 3 files |
| 2 | Repository Port (in/out interfaces) | 3 files |
| 3 | Domain Services | 2 files |
| 4 | JPA Entities | 2 files |
| 5 | Repository Implementation | 1 file |
| 6 | Controller + DTOs | 2 files |
| 7 | Configuration (Law V) | 2 files |
| 8 | Unit Tests | 2 files |

**Total: 17 files to create/replace**

**FIX:** `SettleCommissionUseCase` is now registered as `@Bean` in `CommissionDomainServiceConfig.java`.

All components follow hexagonal architecture:
- Domain layer has ZERO Spring/JPA imports
- Domain services registered via `@Bean` in config (Law V)
- Infrastructure adapters use `@Repository`, `@RestController` (Law VI)
