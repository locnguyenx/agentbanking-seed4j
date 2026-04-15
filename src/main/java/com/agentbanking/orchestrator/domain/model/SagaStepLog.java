package com.agentbanking.orchestrator.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SagaStepLog(
  UUID id,
  SagaExecutionId sagaId,
  String stepName,
  SagaStepStatus status,
  String inputPayload,
  String outputPayload,
  String errorCode,
  String errorMessage,
  Instant startedAt,
  Instant completedAt
) {}