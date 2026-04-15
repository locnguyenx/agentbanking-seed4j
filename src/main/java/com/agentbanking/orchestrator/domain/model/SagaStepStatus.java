package com.agentbanking.orchestrator.domain.model;

public enum SagaStepStatus {
  PENDING,
  RUNNING,
  COMPLETED,
  FAILED,
  COMPENSATING,
  COMPENSATED
}