# 02. Domain Model — Money, Identity, Float, Rules

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all domain model value objects (Money, TransactionId, AgentId, AgentFloat, FeeConfig, VelocityRule) with ZERO framework dependencies.

**Architecture:** Pure Java records in `domain/model/` packages. Shared value objects live under `shared/` with `@SharedKernel`. Bounded context models live under their own context package. Hexagonal layout: `domain/model/`, `domain/port/`, `domain/service/`.

**Tech Stack:** Java 25, JUnit 5, AssertJ, Gradle

**References:**
- BDD: `docs/superpowers/specs/agent-banking-platform-bdd.md` (Ledger & Float, Rules & Fee Engine)
- Design: `docs/superpowers/specs/agent-banking-platform-design.md` (Domain Model)
- Template: `docs/templates/domain-model-template.md`

---

## Task 1: Seed4J Scaffolding

**Purpose:** Use Seed4J CLI to scaffold bounded context packages with hexagonal layout.

**Commands:**

```bash
# List available modules
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar list

# Apply bounded contexts (scaffolds hexagonal package structure)
# For Money (shared module)
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply shared-domain --context money

# For Transaction bounded context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply bounded-context --context transaction

# For Onboarding bounded context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply bounded-context --context onboarding

# For Float bounded context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply bounded-context --context float

# For Rules bounded context
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply bounded-context --context rules
```

**Seed4J scaffolds (DO NOT write manually):**
- Package structure with hexagonal layout (`domain/model/`, `domain/port/in/`, `domain/port/out/`, `domain/service/`, `application/`, `infrastructure/`)
- `@BusinessContext` annotations on `package-info.java`
- Standard test templates with `@UnitTest` annotations
- `@Bean` registration in config classes (Law V)

**After scaffolding, verify:**
- [ ] Package structure created with correct hexagonal layout
- [ ] `@BusinessContext` / `@SharedKernel` annotations present
- [ ] Test templates in place
- [ ] Config classes have `@Bean` methods stubbed

---

## Task 2: Money Value Object (MANUAL — custom business logic)

**BDD Scenarios:** Foundation value object used by all BDD scenarios
**BRD Requirements:** Core domain primitive for all financial calculations
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/shared/money/domain/Money.java`
- Test: `src/test/java/com/agentbanking/shared/money/domain/MoneyTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.agentbanking.shared.money.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class MoneyTest {

  @Nested
  @DisplayName("of")
  class OfTest {

    @Test
    void shouldCreateMoneyWithDefaultCurrency() {
      Money money = Money.of(new BigDecimal("100.00"));

      assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
      assertThat(money.currency()).isEqualTo(Currency.getInstance("MYR"));
    }
  }

  @Nested
  @DisplayName("add")
  class AddTest {

    @Test
    void shouldAddTwoMoneyValuesWithSameCurrency() {
      Money money1 = Money.of(new BigDecimal("100.00"));
      Money money2 = Money.of(new BigDecimal("50.00"));

      Money result = money1.add(money2);

      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void shouldThrowWhenAddingDifferentCurrencies() {
      Money myr = new Money(BigDecimal.TEN, Currency.getInstance("MYR"));
      Money sgd = new Money(BigDecimal.TEN, Currency.getInstance("SGD"));

      assertThatThrownBy(() -> myr.add(sgd))
        .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("subtract")
  class SubtractTest {

    @Test
    void shouldSubtractTwoMoneyValues() {
      Money money1 = Money.of(new BigDecimal("100.00"));
      Money money2 = Money.of(new BigDecimal("30.00"));

      Money result = money1.subtract(money2);

      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void shouldUseHalfUpRounding() {
      Money money1 = Money.of(new BigDecimal("100.00"));
      Money money2 = Money.of(new BigDecimal("33.33"));

      Money result = money1.subtract(money2);

      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("66.67"));
    }
  }

  @Nested
  @DisplayName("comparison")
  class ComparisonTest {

    @Test
    void shouldCheckGreaterThanOrEqual() {
      Money hundred = Money.of(new BigDecimal("100.00"));
      Money fifty = Money.of(new BigDecimal("50.00"));
      Money alsoHundred = Money.of(new BigDecimal("100.00"));

      assertThat(hundred.isGreaterThanOrEqual(fifty)).isTrue();
      assertThat(hundred.isGreaterThanOrEqual(alsoHundred)).isTrue();
      assertThat(fifty.isGreaterThanOrEqual(hundred)).isFalse();
    }

    @Test
    void shouldCheckLessThanOrEqual() {
      Money fifty = Money.of(new BigDecimal("50.00"));
      Money hundred = Money.of(new BigDecimal("100.00"));

      assertThat(fifty.isLessThanOrEqual(hundred)).isTrue();
      assertThat(hundred.isLessThanOrEqual(fifty)).isFalse();
    }

    @Test
    void shouldCheckIsPositive() {
      assertThat(Money.of(new BigDecimal("1.00")).isPositive()).isTrue();
      assertThat(Money.of(BigDecimal.ZERO).isPositive()).isFalse();
      assertThat(Money.of(new BigDecimal("-1.00")).isPositive()).isFalse();
    }

    @Test
    void shouldCheckIsZero() {
      assertThat(Money.of(BigDecimal.ZERO).isZero()).isTrue();
      assertThat(Money.of(new BigDecimal("1.00")).isZero()).isFalse();
    }
  }

  @Nested
  @DisplayName("equalsAndHashCode")
  class EqualsAndHashCodeTest {

    @Test
    void shouldHaveEqualMoney() {
      Money money1 = Money.of(new BigDecimal("100.00"));
      Money money2 = Money.of(new BigDecimal("100.00"));

      assertThat(money1).isEqualTo(money2);
      assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    void shouldHaveDifferentMoney() {
      Money money1 = Money.of(new BigDecimal("100.00"));
      Money money2 = Money.of(new BigDecimal("50.00"));

      assertThat(money1).isNotEqualTo(money2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    void shouldFormatMoney() {
      Money money = Money.of(new BigDecimal("1234.56"));

      assertThat(money.toString()).contains("1234.56");
      assertThat(money.toString()).contains("MYR");
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "MoneyTest" -DfailIfNoTests=false`
Expected: FAIL — class Money not found

- [ ] **Step 3: Write implementation**

```java
package com.agentbanking.shared.money.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

  private static final Currency MYR = Currency.getInstance("MYR");
  private static final int SCALE = 2;

  public static Money of(BigDecimal amount) {
    return new Money(Objects.requireNonNull(amount, "amount cannot be null")
      .setScale(SCALE, RoundingMode.HALF_UP), MYR);
  }

  public Money add(Money other) {
    validateCurrency(other);
    return new Money(
      amount.add(other.amount).setScale(SCALE, RoundingMode.HALF_UP),
      currency
    );
  }

  public Money subtract(Money other) {
    validateCurrency(other);
    return new Money(
      amount.subtract(other.amount).setScale(SCALE, RoundingMode.HALF_UP),
      currency
    );
  }

  public boolean isGreaterThanOrEqual(Money other) {
    validateCurrency(other);
    return amount.compareTo(other.amount) >= 0;
  }

  public boolean isLessThanOrEqual(Money other) {
    validateCurrency(other);
    return amount.compareTo(other.amount) <= 0;
  }

  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  public boolean isZero() {
    return amount.compareTo(BigDecimal.ZERO) == 0;
  }

  private void validateCurrency(Money other) {
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
        "Currency mismatch: " + currency + " vs " + other.currency
      );
    }
  }

  @Override
  public String toString() {
    return "%s %.2f".formatted(currency.getSymbol(), amount);
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "MoneyTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/shared/money/domain/Money.java \
  src/test/java/com/agentbanking/shared/money/domain/MoneyTest.java
git commit -m "feat(domain): add Money value object with HALF_UP rounding"
```

---

## Task 3: Identity Records (MANUAL — custom business logic)

**BDD Scenarios:** Used by Transaction, Onboarding bounded contexts
**BRD Requirements:** Core identity value objects
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/shared/identity/domain/TransactionId.java`
- Create: `src/main/java/com/agentbanking/shared/identity/domain/AgentId.java`
- Test: `src/test/java/com/agentbanking/shared/identity/domain/TransactionIdTest.java`
- Test: `src/test/java/com/agentbanking/shared/identity/domain/AgentIdTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.agentbanking.shared.identity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class TransactionIdTest {

  @Nested
  @DisplayName("generate")
  class GenerateTest {

    @Test
    void shouldGenerateUniqueId() {
      TransactionId id1 = TransactionId.generate();
      TransactionId id2 = TransactionId.generate();

      assertThat(id1.value()).isNotBlank();
      assertThat(id2.value()).isNotBlank();
      assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldGenerateWithPrefix() {
      TransactionId id = TransactionId.generate();

      assertThat(id.value()).startsWith("TXN-");
    }
  }

  @Nested
  @DisplayName("of")
  class OfTest {

    @Test
    void shouldCreateFromValue() {
      TransactionId id = TransactionId.of("TXN-001");

      assertThat(id.value()).isEqualTo("TXN-001");
    }
  }
}
```

```java
package com.agentbanking.shared.identity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class AgentIdTest {

  @Nested
  @DisplayName("of")
  class OfTest {

    @Test
    void shouldCreateFromValue() {
      AgentId id = AgentId.of("AGT-001");

      assertThat(id.value()).isEqualTo("AGT-001");
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
      AgentId id1 = AgentId.of("AGT-001");
      AgentId id2 = AgentId.of("AGT-001");

      assertThat(id1).isEqualTo(id2);
      assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "TransactionIdTest" --tests "AgentIdTest" -DfailIfNoTests=false`
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementations**

**TransactionId.java:**
```java
package com.agentbanking.shared.identity.domain;

import java.util.UUID;

public record TransactionId(String value) {

  public static TransactionId generate() {
    return new TransactionId("TXN-" + UUID.randomUUID());
  }

  public static TransactionId of(String value) {
    return new TransactionId(value);
  }
}
```

**AgentId.java:**
```java
package com.agentbanking.shared.identity.domain;

public record AgentId(String value) {

  public static AgentId of(String value) {
    return new AgentId(value);
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "TransactionIdTest" --tests "AgentIdTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/shared/identity/domain/TransactionId.java \
  src/main/java/com/agentbanking/shared/identity/domain/AgentId.java \
  src/test/java/com/agentbanking/shared/identity/domain/TransactionIdTest.java \
  src/test/java/com/agentbanking/shared/identity/domain/AgentIdTest.java
git commit -m "feat(identity): add TransactionId and AgentId value objects"
```

---

## Task 4: AgentFloat Value Object (MANUAL — custom business logic)

**BDD Scenarios:** BDD-LF01, BDD-LF02 (Ledger & Float)
**BRD Requirements:** Float management for agent transactions
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/float/domain/model/AgentFloat.java`
- Test: `src/test/java/com/agentbanking/float/domain/model/AgentFloatTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.agentbanking.float.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

@UnitTest
class AgentFloatTest {

  @Nested
  @DisplayName("hasSufficientBalance")
  class HasSufficientBalanceTest {

    @Test
    void shouldHaveSufficientBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      assertThat(floatAccount.hasSufficientBalance(amount)).isTrue();
    }

    @Test
    void shouldNotHaveSufficientBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("500.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      assertThat(floatAccount.hasSufficientBalance(amount)).isFalse();
    }
  }

  @Nested
  @DisplayName("debit")
  class DebitTest {

    @Test
    void shouldDebitFromBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat debited = floatAccount.debit(amount);

      assertThat(debited.balance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
      assertThat(debited.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
    }
  }

  @Nested
  @DisplayName("credit")
  class CreditTest {

    @Test
    void shouldCreditToBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat credited = floatAccount.credit(amount);

      assertThat(credited.balance().amount())
        .isEqualByComparingTo(new BigDecimal("6000.00"));
      assertThat(credited.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("6000.00"));
    }
  }

  @Nested
  @DisplayName("reserve")
  class ReserveTest {

    @Test
    void shouldReserveAmount() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat reserved = floatAccount.reserve(amount);

      assertThat(reserved.reservedBalance().amount())
        .isEqualByComparingTo(new BigDecimal("1000.00"));
      assertThat(reserved.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
    }
  }

  @Nested
  @DisplayName("release")
  class ReleaseTest {

    @Test
    void shouldReleaseReservedAmount() {
      AgentFloat floatAccount = anAgentFloatWithBalanceAndReserved("5000.00", "1000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat released = floatAccount.release(amount);

      assertThat(released.reservedBalance().amount())
        .isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(released.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("5000.00"));
    }
  }

  private AgentFloat anAgentFloatWithBalance(String balance) {
    return new AgentFloat(
      AgentId.of("AGT-001"),
      Money.of(new BigDecimal(balance)),
      Money.of(BigDecimal.ZERO),
      Money.of(new BigDecimal(balance)),
      Currency.getInstance("MYR"),
      Instant.now()
    );
  }

  private AgentFloat anAgentFloatWithBalanceAndReserved(String balance, String reserved) {
    Money balanceMoney = Money.of(new BigDecimal(balance));
    Money reservedMoney = Money.of(new BigDecimal(reserved));
    Money availableMoney = Money.of(
      new BigDecimal(balance).subtract(new BigDecimal(reserved))
    );
    return new AgentFloat(
      AgentId.of("AGT-001"),
      balanceMoney,
      reservedMoney,
      availableMoney,
      Currency.getInstance("MYR"),
      Instant.now()
    );
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "AgentFloatTest" -DfailIfNoTests=false`
Expected: FAIL — class not found

- [ ] **Step 3: Write implementation**

```java
package com.agentbanking.float.domain.model;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.Currency;

public record AgentFloat(
  AgentId agentId,
  Money balance,
  Money reservedBalance,
  Money availableBalance,
  Currency currency,
  Instant updatedAt
) {

  public boolean hasSufficientBalance(Money amount) {
    return availableBalance.isGreaterThanOrEqual(amount);
  }

  public AgentFloat debit(Money amount) {
    Money newBalance = balance.subtract(amount);
    Money newAvailable = availableBalance.subtract(amount);
    return new AgentFloat(
      agentId,
      newBalance,
      reservedBalance,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat credit(Money amount) {
    Money newBalance = balance.add(amount);
    Money newAvailable = availableBalance.add(amount);
    return new AgentFloat(
      agentId,
      newBalance,
      reservedBalance,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat reserve(Money amount) {
    Money newReserved = reservedBalance.add(amount);
    Money newAvailable = availableBalance.subtract(amount);
    return new AgentFloat(
      agentId,
      balance,
      newReserved,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat release(Money amount) {
    Money newReserved = reservedBalance.subtract(amount);
    Money newAvailable = availableBalance.add(amount);
    return new AgentFloat(
      agentId,
      balance,
      newReserved,
      newAvailable,
      currency,
      Instant.now()
    );
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "AgentFloatTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/float/domain/model/AgentFloat.java \
  src/test/java/com/agentbanking/float/domain/model/AgentFloatTest.java
git commit -m "feat(float): add AgentFloat value object"
```

---

## Task 5: Fee and Rule Records (MANUAL — custom business logic)

**BDD Scenarios:** BDD-RE01, BDD-RE02 (Rules & Fee Engine)
**BRD Requirements:** Fee configuration and velocity limits for transaction processing
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/rules/domain/model/FeeType.java`
- Create: `src/main/java/com/agentbanking/rules/domain/model/VelocityScope.java`
- Create: `src/main/java/com/agentbanking/rules/domain/model/FeeConfig.java`
- Create: `src/main/java/com/agentbanking/rules/domain/model/VelocityRule.java`
- Test: `src/test/java/com/agentbanking/rules/domain/model/FeeConfigTest.java`
- Test: `src/test/java/com/agentbanking/rules/domain/model/VelocityRuleTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.agentbanking.rules.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;

@UnitTest
class FeeConfigTest {

  @Nested
  @DisplayName("FeeType")
  class FeeTypeTest {

    @Test
    void shouldHaveAllFeeTypes() {
      assertThat(FeeType.values()).hasSize(2);
      assertThat(FeeType.FIXED).isNotNull();
      assertThat(FeeType.PERCENTAGE).isNotNull();
    }
  }

  @Nested
  @DisplayName("isActive")
  class IsActiveTest {

    @Test
    void shouldBeActiveWhenWithinEffectivePeriod() {
      FeeConfig config = aConfigWithEffectivePeriod(
        Instant.now().minus(1, ChronoUnit.DAYS),
        Instant.now().plus(1, ChronoUnit.DAYS)
      );

      assertThat(config.isActive()).isTrue();
    }

    @Test
    void shouldNotBeActiveWhenNotYetEffective() {
      FeeConfig config = aConfigWithEffectivePeriod(
        Instant.now().plus(1, ChronoUnit.DAYS),
        Instant.now().plus(2, ChronoUnit.DAYS)
      );

      assertThat(config.isActive()).isFalse();
    }

    @Test
    void shouldNotBeActiveWhenExpired() {
      FeeConfig config = aConfigWithEffectivePeriod(
        Instant.now().minus(2, ChronoUnit.DAYS),
        Instant.now().minus(1, ChronoUnit.DAYS)
      );

      assertThat(config.isActive()).isFalse();
    }
  }

  private FeeConfig aConfigWithEffectivePeriod(Instant from, Instant to) {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.STANDARD,
      FeeType.FIXED,
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("10000.00")),
      100,
      from,
      to
    );
  }
}
```

```java
package com.agentbanking.rules.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.money.domain.Money;

@UnitTest
class VelocityRuleTest {

  @Nested
  @DisplayName("VelocityScope")
  class VelocityScopeTest {

    @Test
    void shouldHaveAllScopes() {
      assertThat(VelocityScope.values()).hasSize(3);
      assertThat(VelocityScope.GLOBAL).isNotNull();
      assertThat(VelocityScope.PER_NRIC).isNotNull();
      assertThat(VelocityScope.PER_AGENT).isNotNull();
    }
  }

  @Nested
  @DisplayName("isExceeded")
  class IsExceededTest {

    @Test
    void shouldNotBeExceededWhenWithinLimits() {
      VelocityRule rule = aRuleWithLimits(100, "10000.00");

      boolean exceeded = rule.isExceeded(50, Money.of(new BigDecimal("5000.00")));

      assertThat(exceeded).isFalse();
    }

    @Test
    void shouldBeExceededWhenCountExceeds() {
      VelocityRule rule = aRuleWithLimits(100, "10000.00");

      boolean exceeded = rule.isExceeded(101, Money.of(new BigDecimal("5000.00")));

      assertThat(exceeded).isTrue();
    }

    @Test
    void shouldBeExceededWhenAmountExceeds() {
      VelocityRule rule = aRuleWithLimits(100, "10000.00");

      boolean exceeded = rule.isExceeded(50, Money.of(new BigDecimal("10001.00")));

      assertThat(exceeded).isTrue();
    }

    @Test
    void shouldNotBeExceededWhenRuleIsInactive() {
      VelocityRule rule = anInactiveRule();

      boolean exceeded = rule.isExceeded(200, Money.of(new BigDecimal("20000.00")));

      assertThat(exceeded).isFalse();
    }

    private VelocityRule aRuleWithLimits(int maxCount, String maxAmount) {
      return new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        maxCount,
        Money.of(new BigDecimal(maxAmount)),
        true
      );
    }

    private VelocityRule anInactiveRule() {
      return new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        100,
        Money.of(new BigDecimal("10000.00")),
        false
      );
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "FeeConfigTest" --tests "VelocityRuleTest" -DfailIfNoTests=false`
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementations**

**FeeType.java:**
```java
package com.agentbanking.rules.domain.model;

public enum FeeType {
  FIXED,
  PERCENTAGE
}
```

**VelocityScope.java:**
```java
package com.agentbanking.rules.domain.model;

public enum VelocityScope {
  GLOBAL,
  PER_NRIC,
  PER_AGENT
}
```

**FeeConfig.java:**
```java
package com.agentbanking.rules.domain.model;

import com.agentbanking.onboarding.domain.model.AgentType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.transaction.domain.model.TransactionType;
import java.time.Instant;
import java.util.UUID;

public record FeeConfig(
  UUID id,
  TransactionType transactionType,
  AgentType agentTier,
  FeeType feeType,
  Money customerFeeValue,
  Money agentCommissionValue,
  Money bankShareValue,
  Money dailyLimitAmount,
  Integer dailyLimitCount,
  Instant effectiveFrom,
  Instant effectiveTo
) {

  public boolean isActive() {
    Instant now = Instant.now();
    return (effectiveFrom == null || !now.isBefore(effectiveFrom))
      && (effectiveTo == null || !now.isAfter(effectiveTo));
  }

  public boolean isExpired() {
    return effectiveTo != null && Instant.now().isAfter(effectiveTo);
  }
}
```

**VelocityRule.java:**
```java
package com.agentbanking.rules.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.util.UUID;

public record VelocityRule(
  UUID id,
  VelocityScope scope,
  Integer maxTransactionsPerDay,
  Money maxAmountPerDay,
  boolean isActive
) {

  public boolean isExceeded(int currentCount, Money currentAmount) {
    if (!isActive) {
      return false;
    }

    boolean countExceeded = maxTransactionsPerDay != null
      && currentCount >= maxTransactionsPerDay;
    boolean amountExceeded = maxAmountPerDay != null
      && currentAmount.isGreaterThanOrEqual(maxAmountPerDay);

    return countExceeded || amountExceeded;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "FeeConfigTest" --tests "VelocityRuleTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/rules/domain/model/FeeType.java \
  src/main/java/com/agentbanking/rules/domain/model/VelocityScope.java \
  src/main/java/com/agentbanking/rules/domain/model/FeeConfig.java \
  src/main/java/com/agentbanking/rules/domain/model/VelocityRule.java \
  src/test/java/com/agentbanking/rules/domain/model/FeeConfigTest.java \
  src/test/java/com/agentbanking/rules/domain/model/VelocityRuleTest.java
git commit -m "feat(rules): add FeeConfig and VelocityRule domain models"
```

---

## Verification

After completing all tasks, run full test suite:

```bash
./gradlew test --tests "MoneyTest" --tests "TransactionIdTest" --tests "AgentIdTest" --tests "AgentFloatTest" --tests "FeeConfigTest" --tests "VelocityRuleTest"
```

Expected: All tests PASS

---

## Summary

| Task | Domain Objects | Seed4J or Manual | Files Created | Tests |
|------|---------------|-----------------|---------------|-------|
| 1 | Package scaffolding | **Seed4J** | 0 | 0 |
| 2 | Money | **Manual** | 2 | 1 |
| 3 | TransactionId, AgentId | **Manual** | 4 | 2 |
| 4 | AgentFloat | **Manual** | 2 | 1 |
| 5 | FeeType, VelocityScope, FeeConfig, VelocityRule | **Manual** | 6 | 2 |
| **Total** | **8 domain objects** | | **14** | **6** |

All domain models follow hexagonal architecture principles:
- Zero framework imports (no Spring, JPA, Kafka)
- Pure Java records for immutability
- Business logic encapsulated in domain methods
- Comprehensive unit tests with TDD approach
