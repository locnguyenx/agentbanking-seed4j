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