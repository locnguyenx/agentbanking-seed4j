package com.agentbanking.switchadapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InternalTransactionRequest(
    @NotBlank(message = "ERR_VAL_001: Transaction type is required")
    String transactionType,
    @NotBlank(message = "ERR_VAL_002: PAN is required")
    String pan,
    @NotNull(message = "ERR_VAL_003: Amount is required")
    @Positive(message = "ERR_VAL_004: Amount must be positive")
    BigDecimal amount,
    @NotBlank(message = "ERR_VAL_005: Terminal ID is required")
    String terminalId,
    @NotBlank(message = "ERR_VAL_006: Merchant ID is required")
    String merchantId,
    String retrievalReferenceNumber,
    String acquiringInstitutionId
) {}