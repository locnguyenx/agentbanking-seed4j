# Plan 05: Rules & Fee Engine

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Rules & Fee Engine bounded context — fee calculation, transaction validation, and velocity checks using hexagonal architecture.

**Architecture:** Hexagonal (Ports & Adapters). Domain layer has ZERO framework imports. Domain services registered via `@Bean` in config (Law V). Infrastructure adapters use `@Repository`, `@RestController` (Law VI).

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Gradle

**Wave:** 2 (Normal Service — no Temporal)

**Approach:** Seed4J-first. Use Seed4J CLI to scaffold package structure, config classes, Flyway stubs, ArchUnit tests, and test templates. Write custom business logic manually.

---

## Seed4J Scaffolding

Run these Seed4J commands to scaffold the rules bounded context:

```bash
# Apply hexagonal architecture template to rules context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal --context rules

# This creates:
# - src/main/java/com/agentbanking/rules/config/RulesDomainServiceConfig.java
# - src/main/java/com/agentbanking/rules/config/RulesDatabaseConfig.java
# - src/main/java/com/agentbanking/rules/domain/port/in/ (empty)
# - src/main/java/com/agentbanking/rules/domain/port/out/ (empty)
# - src/main/resources/db/migration/rules/ (Flyway stub)
# - ArchUnit test stub
# - Test templates
```

After scaffolding, verify:
- `src/main/java/com/agentbanking/rules/config/RulesDomainServiceConfig.java` exists
- `src/main/java/com/agentbanking/rules/config/RulesDatabaseConfig.java` exists
- `src/test/java/com/agentbanking/rules/` has test stubs

**Existing domain models** (from sub-plan 02, already present):
- `src/main/java/com/agentbanking/rules/domain/model/FeeConfig.java`
- `src/main/java/com/agentbanking/rules/domain/model/VelocityRule.java`
- `src/main/java/com/agentbanking/rules/domain/model/FeeType.java`
- `src/main/java/com/agentbanking/rules/domain/model/VelocityScope.java`

---

## Task 1: Application Layer — Ports (Use Cases)

**Files to create:**
- `src/main/java/com/agentbanking/rules/application/port/in/ValidateTransactionUseCase.java`
- `src/main/java/com/agentbanking/rules/application/port/out/FeeConfigRepository.java`
- `src/main/java/com/agentbanking/rules/application/port/out/VelocityRuleRepository.java`

**ValidateTransactionUseCase.java:**
```java
package com.agentbanking.rules.application.port.in;

import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.domain.application.FeeCalculationResult;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import java.util.List;

public interface ValidateTransactionUseCase {

  ValidationResult validate(TransactionValidationRequest request);

  record TransactionValidationRequest(
    TransactionType type,
    AgentId agentId,
    AgentType agentTier,
    Money amount,
    String mykadNumber
  ) {}

  record ValidationResult(
    boolean valid,
    List<String> errors,
    FeeCalculationResult feeCalculation
  ) {}
}
```

**FeeConfigRepository.java:**
```java
package com.agentbanking.rules.application.port.out;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.transaction.domain.model.TransactionType;
import java.util.List;
import java.util.Optional;

public interface FeeConfigRepository {

  Optional<FeeConfig> findByTransactionTypeAndAgentTier(
    TransactionType type, AgentType tier);

  List<FeeConfig> findAllActive();
}
```

**VelocityRuleRepository.java:**
```java
package com.agentbanking.rules.application.port.out;

import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VelocityRuleRepository {

  List<VelocityRule> findActiveByScope(VelocityScope scope);

  Optional<VelocityRule> findById(UUID id);
}
```

---

## Task 2: Domain Layer — FeeCalculationResult (Value Object)

**File to create:**
- `src/main/java/com/agentbanking/rules/domain/application/FeeCalculationResult.java`

```java
package com.agentbanking.rules.domain.application;

import com.agentbanking.shared.money.domain.Money;

public record FeeCalculationResult(
  Money customerFee,
  Money agentCommission,
  Money bankShare
) {

  public Money total() {
    return customerFee.add(agentCommission).add(bankShare);
  }
}
```

**Test:** `src/test/java/com/agentbanking/rules/domain/application/FeeCalculationResultTest.java`
```java
package com.agentbanking.rules.domain.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.money.domain.Money;

@UnitTest
class FeeCalculationResultTest {

  @Test
  @DisplayName("should create with all values")
  void shouldCreateWithAllValues() {
    Money customerFee = Money.of(new BigDecimal("2.00"));
    Money agentCommission = Money.of(new BigDecimal("1.00"));
    Money bankShare = Money.of(new BigDecimal("1.00"));

    FeeCalculationResult result = new FeeCalculationResult(
      customerFee, agentCommission, bankShare
    );

    assertThat(result.customerFee()).isEqualTo(customerFee);
    assertThat(result.agentCommission()).isEqualTo(agentCommission);
    assertThat(result.bankShare()).isEqualTo(bankShare);
  }

  @Test
  @DisplayName("total should sum all components")
  void totalShouldSumAllComponents() {
    FeeCalculationResult result = new FeeCalculationResult(
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("1.00"))
    );

    Money total = result.total();

    assertThat(total.amount()).isEqualByComparingTo(new BigDecimal("4.00"));
  }
}
```

---

## Task 3: Domain Layer — FeeCalculationService

**File to create:**
- `src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java`

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.domain.application.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;

public class FeeCalculationService {

  public FeeCalculationResult calculate(
    TransactionType transactionType,
    AgentType agentTier,
    Money amount,
    FeeConfig config
  ) {
    Money customerFee;
    Money agentCommission;
    Money bankShare;

    if (config.feeType() == FeeType.FIXED) {
      customerFee = config.customerFeeValue();
      agentCommission = config.agentCommissionValue();
      bankShare = config.bankShareValue();
    } else {
      customerFee = calculatePercentageFee(amount, config.customerFeeValue());
      agentCommission = calculatePercentageFee(amount, config.agentCommissionValue());
      bankShare = calculatePercentageFee(amount, config.bankShareValue());
    }

    return new FeeCalculationResult(customerFee, agentCommission, bankShare);
  }

  private Money calculatePercentageFee(Money amount, Money percentage) {
    return amount.multiply(percentage.amount());
  }
}
```

**Test:** `src/test/java/com/agentbanking/rules/domain/service/FeeCalculationServiceTest.java`
```java
package com.agentbanking.rules.domain.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.domain.application.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;

@UnitTest
class FeeCalculationServiceTest {

  private FeeCalculationService service;

  @BeforeEach
  void setUp() {
    service = new FeeCalculationService();
  }

  @Nested
  @DisplayName("calculate")
  class CalculateTest {

    @Test
    void shouldCalculateFixedFee() {
      FeeConfig config = aFixedFeeConfig();

      FeeCalculationResult result = service.calculate(
        TransactionType.CASH_WITHDRAWAL,
        AgentType.MICRO,
        Money.of(new BigDecimal("500.00")),
        config
      );

      assertThat(result.customerFee().amount())
        .isEqualByComparingTo(new BigDecimal("2.00"));
      assertThat(result.agentCommission().amount())
        .isEqualByComparingTo(new BigDecimal("1.00"));
      assertThat(result.bankShare().amount())
        .isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void shouldCalculatePercentageFee() {
      FeeConfig config = aPercentageFeeConfig();

      FeeCalculationResult result = service.calculate(
        TransactionType.CASH_WITHDRAWAL,
        AgentType.PREMIER,
        Money.of(new BigDecimal("10000.00")),
        config
      );

      assertThat(result.customerFee().amount())
        .isEqualByComparingTo(new BigDecimal("50.00"));
      assertThat(result.agentCommission().amount())
        .isEqualByComparingTo(new BigDecimal("20.00"));
      assertThat(result.bankShare().amount())
        .isEqualByComparingTo(new BigDecimal("30.00"));
    }
  }

  private FeeConfig aFixedFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.MICRO,
      FeeType.FIXED,
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("10000.00")),
      100,
      Instant.now().minus(1, ChronoUnit.DAYS),
      Instant.now().plus(1, ChronoUnit.DAYS)
    );
  }

  private FeeConfig aPercentageFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.PREMIER,
      FeeType.PERCENTAGE,
      Money.of(new BigDecimal("0.005")),
      Money.of(new BigDecimal("0.002")),
      Money.of(new BigDecimal("0.003")),
      Money.of(new BigDecimal("50000.00")),
      200,
      Instant.now().minus(1, ChronoUnit.DAYS),
      Instant.now().plus(1, ChronoUnit.DAYS)
    );
  }
}
```

---

## Task 4: Domain Layer — RulesService

**File to create:**
- `src/main/java/com/agentbanking/rules/domain/service/RulesService.java`

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.TransactionValidationRequest;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.ValidationResult;
import com.agentbanking.rules.application.port.out.FeeConfigRepository;
import com.agentbanking.rules.application.port.out.VelocityRuleRepository;
import com.agentbanking.rules.domain.application.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import java.util.ArrayList;
import java.util.List;

public class RulesService {

  private final FeeConfigRepository feeConfigRepository;
  private final VelocityRuleRepository velocityRuleRepository;
  private final FeeCalculationService feeCalculationService;

  public RulesService(
    FeeConfigRepository feeConfigRepository,
    VelocityRuleRepository velocityRuleRepository,
    FeeCalculationService feeCalculationService
  ) {
    this.feeConfigRepository = feeConfigRepository;
    this.velocityRuleRepository = velocityRuleRepository;
    this.feeCalculationService = feeCalculationService;
  }

  public ValidationResult validateTransaction(TransactionValidationRequest request) {
    List<String> errors = new ArrayList<>();

    if (!request.amount().isPositive()) {
      errors.add("ERR_VAL_004");
      return new ValidationResult(false, errors, null);
    }

    FeeConfig config = feeConfigRepository
      .findByTransactionTypeAndAgentTier(request.type(), request.agentTier())
      .orElse(null);

    if (config == null) {
      errors.add("ERR_VAL_001");
      return new ValidationResult(false, errors, null);
    }

    if (!config.isActive()) {
      errors.add("ERR_VAL_002");
      return new ValidationResult(false, errors, null);
    }

    var feeCalculation = feeCalculationService.calculate(
      request.type(),
      request.agentTier(),
      request.amount(),
      config
    );

    return new ValidationResult(true, List.of(), feeCalculation);
  }

  public void validateDailyLimit(AgentId agentId, TransactionType type, Money amount) {
    // TODO: implement velocity/daily limit checks
  }

  public void validateVelocity(String mykadNumber, Money amount) {
    // TODO: implement velocity checks
  }
}
```

**Test:** `src/test/java/com/agentbanking/rules/domain/service/RulesServiceTest.java`
```java
package com.agentbanking.rules.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.onboarding.domain.model.AgentId;
import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.TransactionValidationRequest;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.ValidationResult;
import com.agentbanking.rules.application.port.out.FeeConfigRepository;
import com.agentbanking.rules.application.port.out.VelocityRuleRepository;
import com.agentbanking.rules.domain.application.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.rules.domain.service.RulesService;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;

@UnitTest
class RulesServiceTest {

  private RulesService service;
  private FeeConfigRepository feeConfigRepository;
  private VelocityRuleRepository velocityRuleRepository;
  private FeeCalculationService feeCalculationService;

  @BeforeEach
  void setUp() {
    feeConfigRepository = mock(FeeConfigRepository.class);
    velocityRuleRepository = mock(VelocityRuleRepository.class);
    feeCalculationService = new FeeCalculationService();
    service = new RulesService(feeConfigRepository, velocityRuleRepository, feeCalculationService);
  }

  @Nested
  @DisplayName("validateTransaction")
  class ValidateTransactionTest {

    @Test
    void shouldReturnValidWithFeeCalculation() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.MICRO,
        Money.of(new BigDecimal("500.00")),
        "123456789012"
      );
      
      FeeConfig config = aValidFeeConfig();
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_WITHDRAWAL, AgentType.MICRO))
        .thenReturn(Optional.of(config));

      ValidationResult result = service.validateTransaction(request);

      assertThat(result.valid()).isTrue();
      assertThat(result.errors()).isEmpty();
      assertThat(result.feeCalculation()).isNotNull();
    }

    @Test
    void shouldReturnInvalidWhenNoFeeConfig() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.MICRO,
        Money.of(new BigDecimal("500.00")),
        "123456789012"
      );
      
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_WITHDRAWAL, AgentType.MICRO))
        .thenReturn(Optional.empty());

      ValidationResult result = service.validateTransaction(request);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).contains("ERR_VAL_001");
    }

    @Test
    void shouldReturnInvalidWhenAmountIsZero() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.MICRO,
        Money.of(BigDecimal.ZERO),
        "123456789012"
      );

      ValidationResult result = service.validateTransaction(request);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).contains("ERR_VAL_004");
    }
  }

  private FeeConfig aValidFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.MICRO,
      FeeType.FIXED,
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("10000.00")),
      100,
      Instant.now().minus(1, ChronoUnit.DAYS),
      Instant.now().plus(1, ChronoUnit.DAYS)
    );
  }
}
```

---

## Task 5: Application Layer — DTOs

**Files to create:**
- `src/main/java/com/agentbanking/rules/application/dto/ValidationRequestDto.java`
- `src/main/java/com/agentbanking/rules/application/dto/ValidationResponseDto.java`
- `src/main/java/com/agentbanking/rules/application/dto/FeeCalculationResultDto.java`

```java
package com.agentbanking.rules.application.dto;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.NotNull;

public record ValidationRequestDto(
  @NotNull TransactionType type,
  @NotNull String agentId,
  @NotNull AgentType agentTier,
  @NotNull Money amount,
  @NotNull String mykadNumber
) {}
```

```java
package com.agentbanking.rules.application.dto;

import java.util.List;

public record ValidationResponseDto(
  boolean valid,
  List<String> errors,
  FeeCalculationResultDto feeCalculation
) {}
```

```java
package com.agentbanking.rules.application.dto;

import com.agentbanking.rules.domain.application.FeeCalculationResult;

public record FeeCalculationResultDto(
  String customerFee,
  String agentCommission,
  String bankShare,
  String total
) {

  public static FeeCalculationResultDto from(FeeCalculationResult result) {
    return new FeeCalculationResultDto(
      result.customerFee().amount().toPlainString(),
      result.agentCommission().amount().toPlainString(),
      result.bankShare().amount().toPlainString(),
      result.total().amount().toPlainString()
    );
  }
}
```

---

## Task 6: Infrastructure Layer — JPA Entities

**Files to create:**
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/FeeConfigEntity.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/VelocityRuleEntity.java`

```java
package com.agentbanking.rules.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_config")
public class FeeConfigEntity {

  @Id
  private UUID id;

  @Column(name = "transaction_type", nullable = false)
  private String transactionType;

  @Column(name = "agent_tier", nullable = false)
  private String agentTier;

  @Column(name = "fee_type", nullable = false)
  private String feeType;

  @Column(name = "customer_fee_value", precision = 19, scale = 4)
  private BigDecimal customerFeeValue;

  @Column(name = "agent_commission_value", precision = 19, scale = 4)
  private BigDecimal agentCommissionValue;

  @Column(name = "bank_share_value", precision = 19, scale = 4)
  private BigDecimal bankShareValue;

  @Column(name = "daily_limit_amount", precision = 19, scale = 2)
  private BigDecimal dailyLimitAmount;

  @Column(name = "daily_limit_count")
  private Integer dailyLimitCount;

  @Column(name = "effective_from")
  private Instant effectiveFrom;

  @Column(name = "effective_to")
  private Instant effectiveTo;

  public FeeConfigEntity() {}

  public static FeeConfigEntity fromDomain(FeeConfig config) {
    FeeConfigEntity entity = new FeeConfigEntity();
    entity.id = config.id();
    entity.transactionType = config.transactionType().name();
    entity.agentTier = config.agentTier().name();
    entity.feeType = config.feeType().name();
    entity.customerFeeValue = config.customerFeeValue().amount();
    entity.agentCommissionValue = config.agentCommissionValue().amount();
    entity.bankShareValue = config.bankShareValue().amount();
    entity.dailyLimitAmount = config.dailyLimitAmount().amount();
    entity.dailyLimitCount = config.dailyLimitCount();
    entity.effectiveFrom = config.effectiveFrom();
    entity.effectiveTo = config.effectiveTo();
    return entity;
  }

  public FeeConfig toDomain() {
    return new FeeConfig(
      id,
      TransactionType.valueOf(transactionType),
      AgentType.valueOf(agentTier),
      FeeType.valueOf(feeType),
      Money.of(customerFeeValue),
      Money.of(agentCommissionValue),
      Money.of(bankShareValue),
      Money.of(dailyLimitAmount),
      dailyLimitCount,
      effectiveFrom,
      effectiveTo
    );
  }
}
```

```java
package com.agentbanking.rules.infrastructure.persistence.entity;

import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.shared.money.domain.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "velocity_rule")
public class VelocityRuleEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String scope;

  @Column(name = "max_transactions_per_day")
  private Integer maxTransactionsPerDay;

  @Column(name = "max_amount_per_day", precision = 19, scale = 2)
  private BigDecimal maxAmountPerDay;

  @Column(nullable = false)
  private boolean isActive;

  public VelocityRuleEntity() {}

  public static VelocityRuleEntity fromDomain(VelocityRule rule) {
    VelocityRuleEntity entity = new VelocityRuleEntity();
    entity.id = rule.id();
    entity.scope = rule.scope().name();
    entity.maxTransactionsPerDay = rule.maxTransactionsPerDay();
    entity.maxAmountPerDay = rule.maxAmountPerDay().amount();
    entity.isActive = rule.isActive();
    return entity;
  }

  public VelocityRule toDomain() {
    return new VelocityRule(
      id,
      VelocityScope.valueOf(scope),
      maxTransactionsPerDay,
      Money.of(maxAmountPerDay),
      isActive
    );
  }
}
```

---

## Task 7: Infrastructure Layer — JPA Repositories

**BUG FIX:** `VelocityRuleRepositoryImpl.findById()` must query `VelocityRuleEntity`, NOT `FeeConfigEntity`.

**Files to create:**
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/FeeConfigRepositoryImpl.java`
- `src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/VelocityRuleRepositoryImpl.java`

```java
package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.rules.application.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import com.agentbanking.transaction.domain.model.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class FeeConfigRepositoryImpl implements FeeConfigRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Optional<FeeConfig> findByTransactionTypeAndAgentTier(
      TransactionType type, AgentType tier) {
    
    var query = entityManager.createQuery(
      "SELECT e FROM FeeConfigEntity e WHERE e.transactionType = :type " +
      "AND e.agentTier = :tier " +
      "AND (e.effectiveFrom IS NULL OR e.effectiveFrom <= :now) " +
      "AND (e.effectiveTo IS NULL OR e.effectiveTo > :now)",
      FeeConfigEntity.class
    );
    
    query.setParameter("type", type.name());
    query.setParameter("tier", tier.name());
    query.setParameter("now", Instant.now());
    
    return query.getResultList().stream().findFirst().map(FeeConfigEntity::toDomain);
  }

  @Override
  public List<FeeConfig> findAllActive() {
    var query = entityManager.createQuery(
      "SELECT e FROM FeeConfigEntity e WHERE " +
      "(e.effectiveFrom IS NULL OR e.effectiveFrom <= :now) " +
      "AND (e.effectiveTo IS NULL OR e.effectiveTo > :now)",
      FeeConfigEntity.class
    );
    
    query.setParameter("now", Instant.now());
    
    return query.getResultList().stream()
      .map(FeeConfigEntity::toDomain)
      .toList();
  }
}
```

```java
package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.application.port.out.VelocityRuleRepository;
import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.rules.infrastructure.persistence.entity.VelocityRuleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VelocityRuleRepositoryImpl implements VelocityRuleRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<VelocityRule> findActiveByScope(VelocityScope scope) {
    var query = entityManager.createQuery(
      "SELECT e FROM VelocityRuleEntity e WHERE e.scope = :scope AND e.isActive = true",
      VelocityRuleEntity.class
    );
    
    query.setParameter("scope", scope.name());
    
    return query.getResultList().stream()
      .map(VelocityRuleEntity::toDomain)
      .toList();
  }

  @Override
  public Optional<VelocityRule> findById(UUID id) {
    // FIXED: queries VelocityRuleEntity (original plan incorrectly queried FeeConfigEntity)
    VelocityRuleEntity entity = entityManager.find(VelocityRuleEntity.class, id);
    return Optional.ofNullable(entity).map(VelocityRuleEntity::toDomain);
  }
}
```

---

## Task 8: Infrastructure Layer — REST Controller

**File to create:**
- `src/main/java/com/agentbanking/rules/infrastructure/primary/RulesController.java`

```java
package com.agentbanking.rules.infrastructure.primary;

import com.agentbanking.rules.application.dto.ValidationRequestDto;
import com.agentbanking.rules.application.dto.ValidationResponseDto;
import com.agentbanking.rules.application.dto.FeeCalculationResultDto;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.TransactionValidationRequest;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase.ValidationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rules")
public class RulesController {

  private final ValidateTransactionUseCase validateUseCase;

  public RulesController(ValidateTransactionUseCase validateUseCase) {
    this.validateUseCase = validateUseCase;
  }

  @PostMapping("/validate")
  public ResponseEntity<ValidationResponseDto> validate(
      @Valid @RequestBody ValidationRequestDto request) {
    
    TransactionValidationRequest domainRequest = new TransactionValidationRequest(
      request.type(),
      com.agentbanking.onboarding.domain.model.AgentId.of(request.agentId()),
      request.agentTier(),
      request.amount(),
      request.mykadNumber()
    );

    ValidationResult result = validateUseCase.validate(domainRequest);

    ValidationResponseDto response = new ValidationResponseDto(
      result.valid(),
      result.errors(),
      result.feeCalculation() != null 
        ? FeeCalculationResultDto.from(result.feeCalculation())
        : null
    );

    return ResponseEntity.ok(response);
  }
}
```

---

## Task 9: Domain Service Registration (Law V)

**File:** `src/main/java/com/agentbanking/rules/config/RulesDomainServiceConfig.java` (scaffolded by Seed4J — add beans)

```java
package com.agentbanking.rules.config;

import com.agentbanking.rules.application.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.application.port.in.ValidateTransactionUseCaseImpl;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import com.agentbanking.rules.domain.service.RulesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RulesDomainServiceConfig {

  @Bean
  public FeeCalculationService feeCalculationService() {
    return new FeeCalculationService();
  }

  @Bean
  public RulesService rulesService(
      com.agentbanking.rules.application.port.out.FeeConfigRepository feeConfigRepository,
      com.agentbanking.rules.application.port.out.VelocityRuleRepository velocityRuleRepository,
      FeeCalculationService feeCalculationService) {
    return new RulesService(
      feeConfigRepository,
      velocityRuleRepository,
      feeCalculationService
    );
  }

  @Bean
  public ValidateTransactionUseCase validateTransactionUseCase(RulesService rulesService) {
    return new ValidateTransactionUseCaseImpl(rulesService);
  }
}
```

---

## Task 10: ValidateTransactionUseCase Implementation

**File to create:**
- `src/main/java/com/agentbanking/rules/application/port/in/ValidateTransactionUseCaseImpl.java`

```java
package com.agentbanking.rules.application.port.in;

import com.agentbanking.rules.domain.service.RulesService;

public class ValidateTransactionUseCaseImpl implements ValidateTransactionUseCase {

  private final RulesService rulesService;

  public ValidateTransactionUseCaseImpl(RulesService rulesService) {
    this.rulesService = rulesService;
  }

  @Override
  public ValidationResult validate(TransactionValidationRequest request) {
    return rulesService.validateTransaction(request);
  }
}
```

---

## Verification

```bash
./gradlew test -Dtest.include="**/rules/**/*Test*"
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files Created | Tests |
|------|-----------|---------------|-------|
| 1 | Application Layer — Ports | 3 | 0 |
| 2 | Domain — FeeCalculationResult | 1 | 1 |
| 3 | Domain — FeeCalculationService | 1 | 1 |
| 4 | Domain — RulesService | 1 | 1 |
| 5 | Application Layer — DTOs | 3 | 0 |
| 6 | Infrastructure — JPA Entities | 2 | 0 |
| 7 | Infrastructure — JPA Repositories | 2 | 0 |
| 8 | Infrastructure — REST Controller | 1 | 0 |
| 9 | Config — Domain Services | 0 (modify scaffolded) | 0 |
| 10 | UseCase Implementation | 1 | 0 |
| **Total** | | **15** | **3** |

**BUG FIX in Task 7:** VelocityRuleRepositoryImpl.findById() now correctly queries VelocityRuleEntity instead of FeeConfigEntity.

All components follow hexagonal architecture:
- Domain layer has ZERO Spring/JPA imports
- Domain services registered via `@Bean` in config (Law V)
- Infrastructure adapters use `@Repository`, `@RestController` (Law VI)
- All DTOs validated with `jakarta.validation`
