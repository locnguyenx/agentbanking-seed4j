package com.agentbanking.prepaid.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TopUpTransaction(
  String transactionId,
  String providerCode,
  String phoneNumber,
  BigDecimal amount,
  TopUpStatus status,
  String confirmationNumber,
  Instant paidAt
) {
  public boolean canBeReversed() {
    return status == TopUpStatus.SUCCESS;
  }
}