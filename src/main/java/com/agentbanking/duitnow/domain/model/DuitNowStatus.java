package com.agentbanking.duitnow.domain.model;

public enum DuitNowStatus {
  PENDING,
  RESOLVING_PROXY,
  PROCESSING,
  COMPLETED,
  FAILED,
  TIMEOUT
}