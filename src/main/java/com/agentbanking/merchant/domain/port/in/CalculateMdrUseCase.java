package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantCategory;
import com.agentbanking.merchant.domain.model.CardType;
import java.math.BigDecimal;
import java.util.Optional;

public interface CalculateMdrUseCase {
  record MdrResult(BigDecimal mdrPercentage, BigDecimal mdrFixed, BigDecimal totalMdr) {}
  Optional<MdrResult> calculate(MerchantCategory category, CardType cardType, BigDecimal amount);
}