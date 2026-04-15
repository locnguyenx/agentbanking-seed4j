package com.agentbanking.orchestrator.infrastructure.external.dto;

public record CreditResultDTO(
  boolean success,
  String transactionId,
  String errorMessage
) {}
