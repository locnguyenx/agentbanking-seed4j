package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.rules.infrastructure.persistence.entity.VelocityRuleEntity;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VelocityRuleRepositoryImpl implements VelocityRuleRepository {

  private final VelocityRuleJpaRepository jpaRepository;

  public VelocityRuleRepositoryImpl(VelocityRuleJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public List<VelocityRule> findByScope(VelocityScope scope) {
    return jpaRepository.findByScopeAndIsActiveTrue(scope.name())
      .stream()
      .map(this::toDomain)
      .toList();
  }

  @Override
  public Optional<VelocityRule> findActiveByScope(VelocityScope scope) {
    return jpaRepository.findByScopeAndIsActiveTrue(scope.name())
      .stream()
      .findFirst()
      .map(this::toDomain);
  }

  private VelocityRule toDomain(VelocityRuleEntity entity) {
    return new VelocityRule(
      entity.getId(),
      VelocityScope.valueOf(entity.getScope()),
      entity.getMaxTransactionsPerDay(),
      Money.of(entity.getMaxAmountPerDay()),
      Boolean.TRUE.equals(entity.getIsActive())
    );
  }
}
