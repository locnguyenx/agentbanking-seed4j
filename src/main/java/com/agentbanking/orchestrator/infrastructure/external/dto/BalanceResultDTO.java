package com.agentbanking.orchestrator.infrastructure.external.dto;

import com.agentbanking.shared.money.domain.Money;

public record BalanceResultDTO(
  Money availableBalance,
  Money reservedBalance,
  Money totalBalance,
  String agentId
) {}
