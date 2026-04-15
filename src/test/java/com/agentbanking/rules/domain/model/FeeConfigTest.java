package com.agentbanking.rules.domain.model;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@UnitTest
class FeeConfigTest {

  @Test
  void isActive_whenEffectiveFromInPastAndNoEndDate_returnsTrue() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.MICRO,
      FeeType.FIXED,
      Money.of(new BigDecimal("5.00")),
      Money.of(new BigDecimal("3.00")),
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().minusDays(10),
      null
    );

    assertTrue(config.isActive());
  }

  @Test
  void isActive_whenEffectiveFromInFuture_returnsFalse() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.MICRO,
      FeeType.FIXED,
      Money.of(new BigDecimal("5.00")),
      Money.of(new BigDecimal("3.00")),
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().plusDays(10),
      null
    );

    assertFalse(config.isActive());
  }

  @Test
  void isActive_whenEffectiveToInPast_returnsFalse() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.MICRO,
      FeeType.FIXED,
      Money.of(new BigDecimal("5.00")),
      Money.of(new BigDecimal("3.00")),
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().minusDays(30),
      LocalDate.now().minusDays(1)
    );

    assertFalse(config.isActive());
  }

  @Test
  void isActive_whenEffectiveFromInPastAndEffectiveToInFuture_returnsTrue() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.STANDARD,
      FeeType.PERCENTAGE,
      Money.of(new BigDecimal("1.50")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("0.50")),
      Money.of(new BigDecimal("20000.00")),
      100,
      LocalDate.now().minusDays(5),
      LocalDate.now().plusDays(30)
    );

    assertTrue(config.isActive());
  }

  @Test
  void isActive_whenEffectiveFromIsToday_returnsTrue() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_DEPOSIT,
      AgentType.PREMIER,
      FeeType.FIXED,
      Money.of(new BigDecimal("3.00")),
      Money.of(new BigDecimal("2.00")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("50000.00")),
      200,
      LocalDate.now(),
      LocalDate.now().plusYears(1)
    );

    assertTrue(config.isActive());
  }

  @Test
  void isActive_whenEffectiveToIsToday_returnsTrue() {
    FeeConfig config = new FeeConfig(
      UUID.randomUUID(),
      TransactionType.BILL_PAYMENT,
      AgentType.MICRO,
      FeeType.PERCENTAGE,
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("0.70")),
      Money.of(new BigDecimal("0.30")),
      Money.of(new BigDecimal("5000.00")),
      20,
      LocalDate.now().minusMonths(6),
      LocalDate.now()
    );

    assertTrue(config.isActive());
  }
}