package com.agentbanking.rules.domain.model;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.money.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@UnitTest
class VelocityRuleTest {

  @Test
  void isExceeded_whenInactive_returnsFalse() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_AGENT,
      10,
      Money.of(new BigDecimal("1000.00")),
      false
    );

    assertFalse(rule.isExceeded(5, Money.of(new BigDecimal("500.00"))));
  }

  @Test
  void isExceeded_whenCountBelowLimit_returnsFalse() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_NRIC,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertFalse(rule.isExceeded(5, Money.of(new BigDecimal("500.00"))));
  }

  @Test
  void isExceeded_whenCountEqualsLimit_returnsTrue() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_NRIC,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertTrue(rule.isExceeded(10, Money.of(new BigDecimal("500.00"))));
  }

  @Test
  void isExceeded_whenCountExceedsLimit_returnsTrue() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.GLOBAL,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertTrue(rule.isExceeded(15, Money.of(new BigDecimal("500.00"))));
  }

  @Test
  void isExceeded_whenAmountBelowLimit_returnsFalse() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_AGENT,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertFalse(rule.isExceeded(3, Money.of(new BigDecimal("999.99"))));
  }

  @Test
  void isExceeded_whenAmountEqualsLimit_returnsTrue() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_NRIC,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertTrue(rule.isExceeded(3, Money.of(new BigDecimal("1000.00"))));
  }

  @Test
  void isExceeded_whenAmountExceedsLimit_returnsTrue() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.GLOBAL,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertTrue(rule.isExceeded(3, Money.of(new BigDecimal("1500.00"))));
  }

  @Test
  void isExceeded_whenCountNotExceededButAmountExceeds_returnsTrue() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_AGENT,
      10,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertTrue(rule.isExceeded(2, Money.of(new BigDecimal("1500.00"))));
  }

  @Test
  void isExceeded_whenMaxTransactionsPerDayIsNull() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_NRIC,
      null,
      Money.of(new BigDecimal("1000.00")),
      true
    );

    assertFalse(rule.isExceeded(100, Money.of(new BigDecimal("500.00"))));
  }

  @Test
  void isExceeded_whenMaxAmountPerDayIsNull() {
    VelocityRule rule = new VelocityRule(
      UUID.randomUUID(),
      VelocityScope.PER_AGENT,
      10,
      null,
      true
    );

    assertFalse(rule.isExceeded(5, Money.of(new BigDecimal("100000.00"))));
  }
}