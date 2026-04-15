package com.agentbanking.duitnow.infrastructure.adapter;

import com.agentbanking.duitnow.domain.model.DuitNowStatus;
import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import com.agentbanking.duitnow.domain.port.out.PayNetGatewayPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MockPayNetGatewayAdapter implements PayNetGatewayPort {

  @Override
  public DuitNowTransaction transfer(UUID transactionId, String targetAccount, 
      String bankCode, BigDecimal amount, String reference) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    return new DuitNowTransaction(
      transactionId, null, null, null, null, null, targetAccount, bankCode, null,
      amount, reference, DuitNowStatus.COMPLETED, Instant.now(), Instant.now(), null
    );
  }

  @Override
  public DuitNowTransaction queryStatus(UUID transactionId) {
    return new DuitNowTransaction(
      transactionId, null, null, null, null, null, null, null, null,
      null, null, DuitNowStatus.COMPLETED, Instant.now(), Instant.now(), null
    );
  }
}