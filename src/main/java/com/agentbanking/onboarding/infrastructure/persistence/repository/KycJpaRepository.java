package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.KycVerificationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycJpaRepository extends JpaRepository<KycVerificationEntity, UUID> {
    Optional<KycVerificationEntity> findByAgentId(UUID agentId);
    List<KycVerificationEntity> findByStatus(String status);
}