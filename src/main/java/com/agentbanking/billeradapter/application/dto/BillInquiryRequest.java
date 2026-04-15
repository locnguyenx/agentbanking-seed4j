package com.agentbanking.billeradapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record BillInquiryRequest(
    @NotBlank(message = "Biller code is required") String billerCode,
    @NotBlank(message = "Reference 1 is required") String ref1,
    String ref2
) {}