package com.agentbanking.billeradapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BillPaymentRequest(
    @NotBlank(message = "Biller code is required") String billerCode,
    @NotBlank(message = "Reference 1 is required") String ref1,
    String ref2,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey
) {}