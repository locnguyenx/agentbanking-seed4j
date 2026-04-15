package com.agentbanking.floatagg.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

public interface DebitFloatUseCase {

  DebitResult debit(AgentId agentId, Money amount, String lockId);
  
  record DebitResult(
    boolean success,
    String debitId,
    String errorCode
  ) {}
}
