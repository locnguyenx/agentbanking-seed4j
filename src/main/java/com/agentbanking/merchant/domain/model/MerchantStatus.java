package com.agentbanking.merchant.domain.model;

public enum MerchantStatus {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  VOIDED,
  REFUNDED,
  FAILED
}