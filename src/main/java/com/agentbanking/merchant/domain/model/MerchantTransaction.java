package com.agentbanking.merchant.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MerchantTransaction(
  UUID id,
  String agentId,
  UUID traceId,
  String merchantId,
  CardType cardType,
  String maskedPan,
  BigDecimal transactionAmount,
  BigDecimal mdrAmount,
  BigDecimal totalAmount,
  BigDecimal cashBackAmount,
  MerchantStatus status,
  String authorizationCode,
  Instant createdAt,
  Instant capturedAt,
  String invoiceNumber,
  String terminalId
) {}