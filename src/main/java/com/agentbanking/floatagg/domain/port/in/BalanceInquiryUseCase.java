package com.agentbanking.floatagg.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

public interface BalanceInquiryUseCase {

  BalanceResult getBalance(AgentId agentId);
  
  record BalanceResult(
    Money balance,
    Money availableBalance,
    Money reservedBalance,
    String currency
  ) {}
}
