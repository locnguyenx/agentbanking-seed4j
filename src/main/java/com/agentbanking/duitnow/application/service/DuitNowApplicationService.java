package com.agentbanking.duitnow.application.service;

import com.agentbanking.duitnow.application.dto.DuitNowTransferRequest;
import com.agentbanking.duitnow.application.dto.DuitNowTransferResponse;
import com.agentbanking.duitnow.domain.model.DuitNowStatus;
import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import com.agentbanking.duitnow.domain.port.in.TransferMoneyUseCase;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

public class DuitNowApplicationService {

  private final TransferMoneyUseCase transferMoneyUseCase;
  private static final Duration TIMEOUT = Duration.ofSeconds(15);
  private final ExecutorService executor;

  public DuitNowApplicationService(TransferMoneyUseCase transferMoneyUseCase) {
    this.transferMoneyUseCase = transferMoneyUseCase;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public DuitNowTransferResponse transfer(DuitNowTransferRequest request, String idempotencyKey) {
    try {
      Future<DuitNowTransaction> future = executor.submit(() -> 
        transferMoneyUseCase.transfer(
          UUID.randomUUID(),
          request.proxyType(),
          request.proxyValue(),
          request.amount(),
          request.reference(),
          idempotencyKey
        )
      );
      
      DuitNowTransaction result = future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      
      return new DuitNowTransferResponse(
        result.id(),
        result.status().name(),
        result.recipientName(),
        result.amount(),
        result.completedAt(),
        "Transfer completed successfully"
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Transfer interrupted");
    } catch (TimeoutException e) {
      throw new RuntimeException("DuitNow transfer timed out after 15 seconds");
    } catch (ExecutionException e) {
      throw new RuntimeException("DuitNow transfer failed: " + e.getCause().getMessage());
    }
  }
}