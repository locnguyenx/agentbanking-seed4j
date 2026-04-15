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
    boolean countExceeded = maxTransactionsPerDay != null && currentCount >= maxTransactionsPerDay;
    boolean amountExceeded = maxAmountPerDay != null && currentAmount.isGreaterThanOrEqual(maxAmountPerDay);
    return countExceeded || amountExceeded;
  }
}