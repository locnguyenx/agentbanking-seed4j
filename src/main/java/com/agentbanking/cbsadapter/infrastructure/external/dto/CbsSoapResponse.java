package com.agentbanking.cbsadapter.infrastructure.external.dto;

public record CbsSoapResponse(
    String responseCode,
    boolean success,
    String balance,
    String message,
    String transactionId,
    String timestamp
) {}