package com.agentbanking.merchant.infrastructure.adapter;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.out.CardGatewayPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MockCardGatewayAdapter implements CardGatewayPort {

  @Override
  public MerchantTransaction authorize(String cardData, BigDecimal amount) {
    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    String authCode = "AUTH" + System.currentTimeMillis() % 100000;
    
    return new MerchantTransaction(
      UUID.randomUUID(), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.AUTHORIZED, authCode, Instant.now(), null, null, null
    );
  }

  @Override
  public MerchantTransaction capture(String transactionId, BigDecimal amount) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.CAPTURED, "CAPTURED", Instant.now(), Instant.now(), null, null
    );
  }

  @Override
  public MerchantTransaction voidTransaction(String transactionId) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
      MerchantStatus.VOIDED, "VOIDED", Instant.now(), null, null, null
    );
  }

  @Override
  public MerchantTransaction refund(String transactionId, BigDecimal amount) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.REFUNDED, "REFUNDED", Instant.now(), Instant.now(), null, null
    );
  }
}