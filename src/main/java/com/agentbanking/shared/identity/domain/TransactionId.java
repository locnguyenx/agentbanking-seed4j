package com.agentbanking.shared.identity.domain;

import java.util.UUID;

public record TransactionId(String value) {

  public static TransactionId generate() {
    return new TransactionId("TXN-" + UUID.randomUUID());
  }

  public static TransactionId of(String value) {
    return new TransactionId(value);
  }
}
