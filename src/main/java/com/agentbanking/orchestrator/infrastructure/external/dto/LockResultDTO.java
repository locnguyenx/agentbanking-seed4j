package com.agentbanking.orchestrator.infrastructure.external.dto;

public record LockResultDTO(
  boolean success,
  String lockId,
  String errorMessage
) {}
