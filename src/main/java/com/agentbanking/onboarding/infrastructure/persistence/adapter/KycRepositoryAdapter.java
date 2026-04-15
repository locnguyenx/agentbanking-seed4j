package com.agentbanking.onboarding.infrastructure.persistence.adapter;

import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.model.KycVerification;
import com.agentbanking.onboarding.domain.port.out.KycRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.KycVerificationEntity;
import com.agentbanking.onboarding.infrastructure.persistence.repository.KycJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class KycRepositoryAdapter implements KycRepository {

    private final KycJpaRepository jpaRepository;

    public KycRepositoryAdapter(KycJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<KycVerification> findByAgentId(UUID agentId) {
        return jpaRepository.findByAgentId(agentId).map(this::toDomain);
    }

    @Override
    public KycVerification save(KycVerification verification) {
        KycVerificationEntity entity = toEntity(verification);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public List<KycVerification> findByStatus(KycStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private KycVerification toDomain(KycVerificationEntity entity) {
        return new KycVerification(
            entity.getId(),
            entity.getAgentId(),
            entity.getStatus(),
            entity.getMykadNumber(),
            entity.getOcrName(),
            entity.getOcrAddress(),
            entity.getBiometricTemplate(),
            entity.getLivenessScore(),
            entity.getAmlStatus(),
            entity.getBiometricMatch(),
            entity.getAge(),
            entity.getSubmittedAt(),
            entity.getVerifiedAt(),
            entity.getRejectionReason()
        );
    }

    private KycVerificationEntity toEntity(KycVerification verification) {
        KycVerificationEntity entity = new KycVerificationEntity();
        entity.setId(verification.id());
        entity.setAgentId(verification.agentId());
        entity.setStatus(verification.status());
        entity.setMykadNumber(verification.mykadNumber());
        entity.setOcrName(verification.ocrName());
        entity.setOcrAddress(verification.ocrAddress());
        entity.setBiometricTemplate(verification.biometricTemplate());
        entity.setLivenessScore(verification.livenessScore());
        entity.setAmlStatus(verification.amlStatus());
        entity.setBiometricMatch(verification.biometricMatch());
        entity.setAge(verification.age());
        entity.setSubmittedAt(verification.submittedAt());
        entity.setVerifiedAt(verification.verifiedAt());
        entity.setRejectionReason(verification.rejectionReason());
        return entity;
    }
}