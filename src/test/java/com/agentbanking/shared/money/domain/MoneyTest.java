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