package com.agentbanking.merchant.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record MdrConfig(
  UUID id,
  MerchantCategory category,
  CardType cardType,
  BigDecimal mdrPercentage,
  BigDecimal mdrFixed,
  BigDecimal minAmount,
  BigDecimal maxAmount,
  boolean cashBackAllowed
) {}