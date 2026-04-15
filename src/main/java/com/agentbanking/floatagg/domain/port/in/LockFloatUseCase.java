package com.agentbanking.floatagg.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

public interface LockFloatUseCase {

  LockResult reserveFloat(AgentId agentId, Money amount, String sagaId);
  
  record LockResult(
    boolean success,
    String lockId,
    String errorCode
  ) {}
}
