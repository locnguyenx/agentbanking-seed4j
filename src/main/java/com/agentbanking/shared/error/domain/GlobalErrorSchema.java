package com.agentbanking.shared.error.domain;

import java.time.Instant;

public record GlobalErrorSchema(
  String code,
  String message,
  String actionCode,
  String traceId,
  Instant timestamp
) {

  public static GlobalErrorSchema of(String code, String message, String actionCode) {
    return new GlobalErrorSchema(
      code,
      message,
      actionCode,
      generateTraceId(),
      Instant.now()
    );
  }

  private static String generateTraceId() {
    return "trace-" + System.currentTimeMillis();
  }
}