package com.agentbanking.hsmadapter.infrastructure.external.dto;

public record HsmCommandResponse(
    String responseCode,
    boolean success,
    String data,
    String pinBlock,
    int remainingAttempts,
    String timestamp
) {}