package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.BalanceResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.CreditResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.DebitResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.LockResultDTO;
import org.springframework.stereotype.Component;

@Component
public class FloatClientFallback implements FloatClient {

  @Override
  public LockResultDTO reserveFloat(String agentId, Object request) {
    return new LockResultDTO(false, null, "ERR_EXT_101");
  }

  @Override
  public DebitResultDTO debit(String agentId, Object request) {
    return new DebitResultDTO(false, null, "ERR_EXT_101");
  }

  @Override
  public CreditResultDTO credit(String agentId, Object request) {
    return new CreditResultDTO(false, null, "ERR_EXT_101");
  }

  @Override
  public BalanceResultDTO getBalance(String agentId) {
    return new BalanceResultDTO(null, null, null, agentId);
  }

  @Override
  public void releaseLock(String lockId) {
    throw new RuntimeException("ERR_EXT_101: Float service unavailable");
  }
}
