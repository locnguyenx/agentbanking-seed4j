package com.agentbanking.transaction.domain.model;

public enum TransactionStatus {
  PENDING,
  COMPLETED,
  FAILED,
  REVERSAL_INITIATED,
  REVERSED
}