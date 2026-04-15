package com.agentbanking.billeradapter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BillerTransaction(
  String transactionId,
  String billerCode,
  String ref1,
  String ref2,
  BigDecimal amount,
  BillerStatus status,
  String customerName,
  Instant paidAt
) {
  public boolean canBeReversed() {
    return status == BillerStatus.CONFIRMED;
  }
}