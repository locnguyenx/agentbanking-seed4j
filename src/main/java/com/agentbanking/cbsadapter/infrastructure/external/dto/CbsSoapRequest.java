package com.agentbanking.cbsadapter.infrastructure.external.dto;

public record CbsSoapRequest(
    String accountId,
    String transactionType,
    String amount,
    String reference,
    String traceId
) {}