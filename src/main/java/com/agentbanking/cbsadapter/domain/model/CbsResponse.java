package com.agentbanking.cbsadapter.domain.model;

import java.time.Instant;

public record CbsResponse(
  boolean success,
  String responseCode,
  String responseMessage,
  String balance,
  String transactionId,
  Instant timestamp
) {
  public static CbsResponse failure(String errorCode, String message) {
    return new CbsResponse(false, errorCode, message, null, null, Instant.now());
  }
}
