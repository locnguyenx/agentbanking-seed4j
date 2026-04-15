package com.agentbanking.duitnow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DuitNowTransferRequest(
  @NotBlank String proxyType,
  @NotBlank String proxyValue,
  @NotNull @Positive BigDecimal amount,
  @NotBlank String reference
) {}