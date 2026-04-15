package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FeeConfigRepositoryImpl implements FeeConfigRepository {

  private final FeeConfigJpaRepository jpaRepository;

  public FeeConfigRepositoryImpl(FeeConfigJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<FeeConfig> findByTransactionTypeAndAgentTier(TransactionType type, AgentType tier) {
    return jpaRepository.findByTransactionTypeAndAgentTier(type.name(), tier.name())
      .stream()
      .findFirst()
      .map(this::toDomain);
  }

  private FeeConfig toDomain(FeeConfigEntity entity) {
    return new FeeConfig(
      entity.getId(),
      TransactionType.valueOf(entity.getTransactionType()),
      AgentType.valueOf(entity.getAgentTier()),
      FeeType.valueOf(entity.getFeeType()),
      Money.of(entity.getCustomerFeeValue()),
      Money.of(entity.getAgentCommissionValue()),
      Money.of(entity.getBankShareValue()),
      Money.of(entity.getDailyLimitAmount()),
      entity.getDailyLimitCount(),
      entity.getEffectiveFrom(),
      entity.getEffectiveTo()
    );
  }
}
