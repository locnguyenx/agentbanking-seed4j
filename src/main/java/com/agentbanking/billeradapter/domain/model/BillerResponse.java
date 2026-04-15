package com.agentbanking.billeradapter.domain.model;

import java.time.Instant;

public record BillerResponse(
  boolean success,
  String responseCode,
  String responseMessage,
  String transactionId,
  String referenceNumber,
  Instant timestamp
) {}
