package com.agentbanking.floatagg.infrastructure.persistence.repository;

import com.agentbanking.floatagg.domain.model.AgentFloat;
import com.agentbanking.floatagg.domain.port.out.AgentFloatRepository;
import com.agentbanking.floatagg.infrastructure.persistence.entity.AgentFloatEntity;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;

@Repository
public class AgentFloatRepositoryImpl implements AgentFloatRepository {

  private final AgentFloatJpaRepository jpaRepository;

  public AgentFloatRepositoryImpl(AgentFloatJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<AgentFloat> findByAgentId(AgentId agentId) {
    return jpaRepository.findByAgentId(agentId.value())
      .map(this::toDomain);
  }

  @Override
  public AgentFloat save(AgentFloat agentFloat) {
    AgentFloatEntity entity = toEntity(agentFloat);
    AgentFloatEntity saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  private AgentFloat toDomain(AgentFloatEntity entity) {
    return new AgentFloat(
      AgentId.of(entity.getAgentId()),
      Money.of(entity.getBalance()),
      Money.of(entity.getReservedBalance()),
      Money.of(entity.getAvailableBalance()),
      Currency.getInstance(entity.getCurrency()),
      entity.getUpdatedAt()
    );
  }

  private AgentFloatEntity toEntity(AgentFloat agentFloat) {
    AgentFloatEntity entity = new AgentFloatEntity();
    entity.setAgentId(agentFloat.agentId().value());
    entity.setBalance(agentFloat.balance().amount());
    entity.setReservedBalance(agentFloat.reservedBalance().amount());
    entity.setAvailableBalance(agentFloat.availableBalance().amount());
    entity.setCurrency(agentFloat.currency().getCurrencyCode());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }
}
