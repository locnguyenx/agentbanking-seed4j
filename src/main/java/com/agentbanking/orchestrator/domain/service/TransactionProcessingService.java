package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.TransactionRequest;
import com.agentbanking.orchestrator.domain.model.TransactionResponse;
import com.agentbanking.shared.transaction.domain.TransactionStatus;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;

public class TransactionProcessingService {

  public TransactionResponse processTransaction(TransactionRequest request) {
    SagaExecutionId sagaId = SagaExecutionId.generate();
    
    return new TransactionResponse(
      sagaId,
      TransactionStatus.PENDING,
      request.amount(),
      null,
      null,
      Instant.now(),
      null
    );
  }

  public TransactionResponse getTransactionStatus(String sagaId) {
    return new TransactionResponse(
      SagaExecutionId.of(sagaId),
      TransactionStatus.PENDING,
      Money.of(java.math.BigDecimal.ZERO),
      null,
      null,
      Instant.now(),
      null
    );
  }

  public boolean cancelTransaction(String sagaId) {
    return true;
  }
}
