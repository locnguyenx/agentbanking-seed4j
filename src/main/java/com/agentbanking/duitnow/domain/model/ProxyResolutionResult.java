package com.agentbanking.duitnow.domain.model;

import java.util.UUID;

public record ProxyResolutionResult(
  UUID transactionId,
  String resolvedAccountNumber,
  String bankCode,
  String recipientName,
  ProxyType proxyType,
  boolean isVerified
) {}