package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FeeCalculationService {

  public FeeCalculationResult calculate(FeeConfig config, Money transactionAmount) {
    if (!config.isActive()) {
      return new FeeCalculationResult(
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        false
      );
    }

    Money customerFee;
    Money agentCommission;
    Money bankShare;

    if (config.feeType() == FeeType.FIXED) {
      customerFee = config.customerFeeValue();
      agentCommission = config.agentCommissionValue();
      bankShare = config.bankShareValue();
    } else {
      BigDecimal customerPercentage = config.customerFeeValue().amount()
          .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
      customerFee = Money.of(
          transactionAmount.amount()
              .multiply(customerPercentage)
              .setScale(2, RoundingMode.HALF_UP));
      
      BigDecimal agentPercentage = config.agentCommissionValue().amount()
          .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
      agentCommission = Money.of(
          transactionAmount.amount()
              .multiply(agentPercentage)
              .setScale(2, RoundingMode.HALF_UP));
      
      BigDecimal bankPercentage = config.bankShareValue().amount()
          .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
      bankShare = Money.of(
          transactionAmount.amount()
              .multiply(bankPercentage)
              .setScale(2, RoundingMode.HALF_UP));
    }

    return new FeeCalculationResult(customerFee, agentCommission, bankShare, true);
  }
}