package com.agentbanking.merchant.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RetailSaleResponse(
  UUID transactionId,
  String status,
  String authorizationCode,
  BigDecimal transactionAmount,
  BigDecimal mdrAmount,
  BigDecimal totalAmount,
  BigDecimal cashBackAmount,
  Instant capturedAt
) {}