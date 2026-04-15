package com.agentbanking.hsmadapter.domain.model;

public record PinVerificationResult(
  boolean verified,
  int remainingAttempts,
  String errorCode
) {}
