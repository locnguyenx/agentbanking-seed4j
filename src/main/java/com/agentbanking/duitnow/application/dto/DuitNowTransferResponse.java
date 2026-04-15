package com.agentbanking.duitnow.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DuitNowTransferResponse(
  UUID transactionId,
  String status,
  String recipientName,
  BigDecimal amount,
  Instant completedAt,
  String message
) {}