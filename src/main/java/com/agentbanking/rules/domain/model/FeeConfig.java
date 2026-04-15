package com.agentbanking.rules.domain.model;

import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;

import java.time.LocalDate;
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
  LocalDate effectiveFrom,
  LocalDate effectiveTo
) {
  public boolean isActive() {
    LocalDate today = LocalDate.now();
    boolean notExpired = effectiveTo == null || !today.isAfter(effectiveTo);
    boolean started = effectiveFrom == null || !today.isBefore(effectiveFrom);
    return notExpired && started;
  }

  public boolean isExpired() {
    return effectiveTo != null && LocalDate.now().isAfter(effectiveTo);
  }
}