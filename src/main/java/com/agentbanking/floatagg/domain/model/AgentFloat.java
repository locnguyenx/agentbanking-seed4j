package com.agentbanking.floatagg.domain.model;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.Currency;

public record AgentFloat(
  AgentId agentId,
  Money balance,
  Money reservedBalance,
  Money availableBalance,
  Currency currency,
  Instant updatedAt
) {

  public boolean hasSufficientBalance(Money amount) {
    return availableBalance.isGreaterThanOrEqual(amount);
  }

  public AgentFloat debit(Money amount) {
    Money newBalance = balance.subtract(amount);
    Money newAvailable = availableBalance.subtract(amount);
    return new AgentFloat(
      agentId,
      newBalance,
      reservedBalance,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat credit(Money amount) {
    Money newBalance = balance.add(amount);
    Money newAvailable = availableBalance.add(amount);
    return new AgentFloat(
      agentId,
      newBalance,
      reservedBalance,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat reserve(Money amount) {
    Money newReserved = reservedBalance.add(amount);
    Money newAvailable = availableBalance.subtract(amount);
    return new AgentFloat(
      agentId,
      balance,
      newReserved,
      newAvailable,
      currency,
      Instant.now()
    );
  }

  public AgentFloat release(Money amount) {
    Money newReserved = reservedBalance.subtract(amount);
    Money newAvailable = availableBalance.add(amount);
    return new AgentFloat(
      agentId,
      balance,
      newReserved,
      newAvailable,
      currency,
      Instant.now()
    );
  }
}
