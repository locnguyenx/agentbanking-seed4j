package com.agentbanking.shared.transaction.domain;

public enum TransactionStatus {
  PENDING,
  COMPLETED,
  FAILED,
  REVERSAL_INITIATED,
  REVERSED
}
