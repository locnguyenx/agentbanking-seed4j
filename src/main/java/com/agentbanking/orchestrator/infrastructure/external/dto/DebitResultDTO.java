package com.agentbanking.orchestrator.infrastructure.external.dto;

public record DebitResultDTO(
  boolean success,
  String transactionId,
  String errorMessage
) {}
