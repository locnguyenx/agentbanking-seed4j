package com.agentbanking.onboarding.infrastructure.persistence.adapter;

import com.agentbanking.onboarding.domain.model.Agent;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import com.agentbanking.onboarding.infrastructure.persistence.repository.AgentJpaRepository;
import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.shared.onboarding.domain.AgentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AgentRepositoryAdapter implements AgentRepository {

    private final AgentJpaRepository jpaRepository;

    public AgentRepositoryAdapter(AgentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Agent> findById(UUID agentId) {
        return jpaRepository.findById(agentId.toString()).map(this::toDomain);
    }

    @Override
    public Optional<Agent> findByMykadNumber(String mykadNumber) {
        return jpaRepository.findByMykadNumber(mykadNumber).map(this::toDomain);
    }

    @Override
    public Agent save(Agent agent) {
        AgentEntity entity = toEntity(agent);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public boolean existsByMykadNumber(String mykadNumber) {
        return jpaRepository.existsByMykadNumber(mykadNumber);
    }

    @Override
    public List<Agent> findByStatus(AgentStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private Agent toDomain(AgentEntity entity) {
        return new Agent(
            UUID.fromString(entity.getId()),
            entity.getAgentCode(),
            AgentType.valueOf(entity.getType()),
            AgentStatus.valueOf(entity.getStatus()),
            entity.getName(),
            entity.getBusinessRegistrationNumber(),
            entity.getOwnerName(),
            entity.getMykadNumber(),
            entity.getPhoneNumber(),
            entity.getEmail(),
            entity.getAddress(),
            entity.getRegisteredAt(),
            entity.getApprovedAt()
        );
    }

    private AgentEntity toEntity(Agent agent) {
        AgentEntity entity = new AgentEntity();
        if (agent.id() != null) {
            entity.setId(agent.id().toString());
        }
        entity.setAgentCode(agent.agentCode());
        entity.setType(agent.agentType().name());
        entity.setStatus(agent.status().name());
        entity.setName(agent.businessName());
        entity.setBusinessRegistrationNumber(agent.businessRegistrationNumber());
        entity.setOwnerName(agent.ownerName());
        entity.setMykadNumber(agent.mykadNumber());
        entity.setPhoneNumber(agent.phoneNumber());
        entity.setEmail(agent.email());
        entity.setAddress(agent.address());
        entity.setRegisteredAt(agent.registeredAt());
        entity.setApprovedAt(agent.approvedAt());
        return entity;
    }
}