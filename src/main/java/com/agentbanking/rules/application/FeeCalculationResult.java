package com.agentbanking.rules.application;

import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;

public record FeeCalculationResult(
  Money customerFee,
  Money agentCommission,
  Money bankShare,
  boolean isValid
) {}
