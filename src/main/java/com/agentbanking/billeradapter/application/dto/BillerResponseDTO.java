package com.agentbanking.billeradapter.application.dto;

import java.time.Instant;

public record BillerResponseDTO(
  boolean success,
  String responseCode,
  String responseMessage,
  String transactionId,
  String referenceNumber,
  Instant timestamp
) {
  public static BillerResponseDTO fromDomain(com.agentbanking.billeradapter.domain.model.BillerResponse domain) {
    return new BillerResponseDTO(
      domain.success(),
      domain.responseCode(),
      domain.responseMessage(),
      domain.transactionId(),
      domain.referenceNumber(),
      domain.timestamp()
    );
  }
}