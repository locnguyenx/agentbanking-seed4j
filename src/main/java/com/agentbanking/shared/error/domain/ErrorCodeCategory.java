package com.agentbanking.shared.error.domain;

public enum ErrorCodeCategory {
  VALIDATION,
  BUSINESS,
  EXTERNAL,
  AUTHENTICATION,
  SYSTEM,
  ISO_TRANSLATION,
  CBS_CONNECTOR,
  HSM_WRAPPER,
  BILLER_GATEWAY,
  ERROR_MAPPING
}