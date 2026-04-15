package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public class CommissionCalculationService implements CalculateCommissionUseCase {

  private final CommissionRepository commissionRepository;

  public CommissionCalculationService(CommissionRepository commissionRepository) {
    this.commissionRepository = commissionRepository;
  }

  @Override
  public CommissionEntry calculate(String transactionId, String agentId, 
                                    String type, Money amount) {
    CommissionType commissionType = CommissionType.valueOf(type);
    Money commissionAmount = calculateCommissionAmount(commissionType, amount);
    
    CommissionEntry entry = new CommissionEntry(
      UUID.randomUUID(),
      transactionId,
      agentId,
      commissionType,
      amount,
      commissionAmount,
      CommissionStatus.CALCULATED,
      Instant.now(),
      null
    );
    
    return commissionRepository.save(entry);
  }

  private Money calculateCommissionAmount(CommissionType type, Money amount) {
    java.math.BigDecimal rate = switch (type) {
      case CASH_WITHDRAWAL -> new java.math.BigDecimal("0.01");
      case CASH_DEPOSIT -> new java.math.BigDecimal("0.005");
      case BILL_PAYMENT -> new java.math.BigDecimal("0.008");
    };
    
    java.math.BigDecimal commission = amount.amount()
        .multiply(rate)
        .setScale(2, java.math.RoundingMode.HALF_UP);
    
    return Money.of(commission);
  }
}
