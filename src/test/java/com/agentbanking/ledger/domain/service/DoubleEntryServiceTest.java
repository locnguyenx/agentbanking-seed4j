package com.agentbanking.ledger.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
@DisplayName("DoubleEntryService")
class DoubleEntryServiceTest {

  private DoubleEntryService doubleEntryService;

  @BeforeEach
  void setUp() {
    doubleEntryService = new DoubleEntryService();
  }

  @Nested
  class ValidateDoubleEntry {

    @Test
    void shouldReturnTrueWhenAmountsMatch() {
      Money amount = Money.of(new BigDecimal("100.00"));

      boolean result = doubleEntryService.validateDoubleEntry(amount, amount);

      assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenAmountsMatchWithDifferentObjects() {
      Money debitAmount = Money.of(new BigDecimal("100.00"));
      Money creditAmount = Money.of(new BigDecimal("100.00"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDebitGreaterThanCredit() {
      Money debitAmount = Money.of(new BigDecimal("150.00"));
      Money creditAmount = Money.of(new BigDecimal("100.00"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCreditGreaterThanDebit() {
      Money debitAmount = Money.of(new BigDecimal("100.00"));
      Money creditAmount = Money.of(new BigDecimal("150.00"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueWhenBothAmountsAreZero() {
      Money zeroAmount = Money.of(BigDecimal.ZERO);

      boolean result = doubleEntryService.validateDoubleEntry(zeroAmount, zeroAmount);

      assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForSmallFractionalAmounts() {
      Money debitAmount = Money.of(new BigDecimal("0.01"));
      Money creditAmount = Money.of(new BigDecimal("0.01"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForSmallDifferenceInAmounts() {
      Money debitAmount = Money.of(new BigDecimal("100.00"));
      Money creditAmount = Money.of(new BigDecimal("100.01"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueForLargeMatchingAmounts() {
      Money debitAmount = Money.of(new BigDecimal("999999.99"));
      Money creditAmount = Money.of(new BigDecimal("999999.99"));

      boolean result = doubleEntryService.validateDoubleEntry(debitAmount, creditAmount);

      assertThat(result).isTrue();
    }
  }
}
