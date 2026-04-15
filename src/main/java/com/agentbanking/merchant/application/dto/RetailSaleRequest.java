package com.agentbanking.merchant.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RetailSaleRequest(
  @NotBlank String merchantId,
  @NotBlank String cardData,
  @NotNull @Positive BigDecimal amount,
  @NotBlank String invoiceNumber
) {}