package com.agentbanking.commission.application.dto;

import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record CommissionEntryDTO(
  UUID id,
  String transactionId,
  String agentId,
  CommissionType type,
  Money transactionAmount,
  Money commissionAmount,
  CommissionStatus status,
  Instant createdAt,
  Instant settledAt
) {
  public static CommissionEntryDTO fromDomain(com.agentbanking.commission.domain.model.CommissionEntry entry) {
    return new CommissionEntryDTO(
      entry.id(),
      entry.transactionId(),
      entry.agentId(),
      entry.type(),
      entry.transactionAmount(),
      entry.commissionAmount(),
      entry.status(),
      entry.createdAt(),
      entry.settledAt()
    );
  }
}
