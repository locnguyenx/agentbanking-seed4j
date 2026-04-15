package com.agentbanking.commission.domain.service;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.port.in.SettleCommissionUseCase;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import java.time.Instant;
import java.util.List;

public class CommissionSettlementService implements SettleCommissionUseCase {

  private final CommissionRepository commissionRepository;

  public CommissionSettlementService(CommissionRepository commissionRepository) {
    this.commissionRepository = commissionRepository;
  }

  @Override
  public int settlePendingCommissions() {
    List<CommissionEntry> pending = commissionRepository.findBySettledAtIsNull();
    
    int settled = 0;
    for (CommissionEntry entry : pending) {
      CommissionEntry settledEntry = new CommissionEntry(
        entry.id(),
        entry.transactionId(),
        entry.agentId(),
        entry.type(),
        entry.transactionAmount(),
        entry.commissionAmount(),
        CommissionStatus.SETTLED,
        entry.createdAt(),
        Instant.now()
      );
      commissionRepository.save(settledEntry);
      settled++;
    }
    
    return settled;
  }
}
