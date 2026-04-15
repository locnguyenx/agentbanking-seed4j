package com.agentbanking.isoadapter.infrastructure.external.dto;

public record IsoTransactionResponse(
    String responseCode,
    boolean success,
    String stan,
    String rawMessage
) {}