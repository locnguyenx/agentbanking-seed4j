package com.agentbanking.billeradapter.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentResponse(
    String code,
    boolean success,
    String transactionId,
    String confirmationNumber,
    BigDecimal amount,
    String message,
    Instant timestamp
) {
  public static BillPaymentResponse success(String transactionId, String confirmationNumber, BigDecimal amount) {
    return new BillPaymentResponse("SUCCESS", true, transactionId, confirmationNumber,
        amount, "Payment successful", Instant.now());
  }

  public static BillPaymentResponse failure(String errorCode, String message) {
    return new BillPaymentResponse(errorCode, false, null, null, null, message, Instant.now());
  }
}