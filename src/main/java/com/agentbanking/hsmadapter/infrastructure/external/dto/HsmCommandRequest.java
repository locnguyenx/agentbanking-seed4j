package com.agentbanking.hsmadapter.infrastructure.external.dto;

public record HsmCommandRequest(
    String command,
    String pinBlock,
    String accountNumber,
    String traceId
) {}