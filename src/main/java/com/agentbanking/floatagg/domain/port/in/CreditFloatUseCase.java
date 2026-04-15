package com.agentbanking.floatagg.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

public interface CreditFloatUseCase {

  CreditResult credit(AgentId agentId, Money amount);
  
  record CreditResult(
    boolean success,
    String creditId,
    String errorCode
  ) {}
}
