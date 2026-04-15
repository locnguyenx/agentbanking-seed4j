package com.agentbanking.duitnow.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DuitNowTransaction(
  UUID id,
  String idempotencyKey,
  String agentId,
  UUID traceId,
  ProxyType proxyType,
  String proxyValue,
  String resolvedAccountNumber,
  String bankCode,
  String recipientName,
  BigDecimal amount,
  String reference,
  DuitNowStatus status,
  Instant createdAt,
  Instant completedAt,
  String failureReason
) {}