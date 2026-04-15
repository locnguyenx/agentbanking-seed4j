package com.agentbanking.rules.domain.model;

import com.agentbanking.shared.money.domain.Money;

public record FeeCalculationResult(
  Money customerFee,
  Money agentCommission,
  Money bankShare,
  boolean isValid
) {}
