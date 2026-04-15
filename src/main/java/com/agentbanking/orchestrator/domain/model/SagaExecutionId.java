package com.agentbanking.orchestrator.domain.model;

import java.util.UUID;

public record SagaExecutionId(String value) {

  public static SagaExecutionId generate() {
    return new SagaExecutionId("SAGA-" + UUID.randomUUID());
  }

  public static SagaExecutionId of(String value) {
    return new SagaExecutionId(value);
  }
}