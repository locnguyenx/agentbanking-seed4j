package com.agentbanking.floatagg.domain.service;

import com.agentbanking.floatagg.domain.model.AgentFloat;
import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase.BalanceResult;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase.CreditResult;
import com.agentbanking.floatagg.domain.port.in.DebitFloatUseCase;
import com.agentbanking.floatagg.domain.port.in.DebitFloatUseCase.DebitResult;
import com.agentbanking.floatagg.domain.port.in.LockFloatUseCase;
import com.agentbanking.floatagg.domain.port.in.LockFloatUseCase.LockResult;
import com.agentbanking.floatagg.domain.port.out.AgentFloatRepository;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.util.UUID;

public class FloatService implements LockFloatUseCase, DebitFloatUseCase, CreditFloatUseCase, BalanceInquiryUseCase {

  private final AgentFloatRepository agentFloatRepository;

  public FloatService(AgentFloatRepository agentFloatRepository) {
    this.agentFloatRepository = agentFloatRepository;
  }

  @Override
  public LockResult reserveFloat(AgentId agentId, Money amount, String sagaId) {
    var floatOpt = agentFloatRepository.findByAgentId(agentId);
    if (floatOpt.isEmpty()) {
      return new LockResult(false, null, "ERR_BIZ_206");
    }
    
    AgentFloat floatAccount = floatOpt.get();
    if (!floatAccount.hasSufficientBalance(amount)) {
      return new LockResult(false, null, "ERR_BIZ_201");
    }
    
    AgentFloat reserved = floatAccount.reserve(amount);
    agentFloatRepository.save(reserved);
    
    return new LockResult(true, "LOCK-" + UUID.randomUUID(), null);
  }

  @Override
  public DebitResult debit(AgentId agentId, Money amount, String lockId) {
    var floatOpt = agentFloatRepository.findByAgentId(agentId);
    if (floatOpt.isEmpty()) {
      return new DebitResult(false, null, "ERR_BIZ_206");
    }
    
    AgentFloat floatAccount = floatOpt.get();
    AgentFloat debited = floatAccount.debit(amount);
    agentFloatRepository.save(debited);
    
    return new DebitResult(true, "DEBIT-" + UUID.randomUUID(), null);
  }

  @Override
  public CreditResult credit(AgentId agentId, Money amount) {
    var floatOpt = agentFloatRepository.findByAgentId(agentId);
    AgentFloat floatAccount;
    
    if (floatOpt.isEmpty()) {
      floatAccount = new AgentFloat(
        agentId,
        amount,
        Money.of(java.math.BigDecimal.ZERO),
        amount,
        java.util.Currency.getInstance("MYR"),
        java.time.Instant.now()
      );
    } else {
      floatAccount = floatOpt.get().credit(amount);
    }
    
    agentFloatRepository.save(floatAccount);
    return new CreditResult(true, "CREDIT-" + UUID.randomUUID(), null);
  }

  @Override
  public BalanceResult getBalance(AgentId agentId) {
    var floatOpt = agentFloatRepository.findByAgentId(agentId);
    if (floatOpt.isEmpty()) {
      return new BalanceResult(
        Money.of(java.math.BigDecimal.ZERO),
        Money.of(java.math.BigDecimal.ZERO),
        Money.of(java.math.BigDecimal.ZERO),
        "MYR"
      );
    }
    
    AgentFloat floatAccount = floatOpt.get();
    return new BalanceResult(
      floatAccount.balance(),
      floatAccount.availableBalance(),
      floatAccount.reservedBalance(),
      floatAccount.currency().getCurrencyCode()
    );
  }
}