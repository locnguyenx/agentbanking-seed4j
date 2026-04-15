package com.agentbanking.onboarding.domain.model;

import java.time.Instant;
import java.util.UUID;

public record KycVerification(
    UUID id,
    UUID agentId,
    KycStatus status,
    String mykadNumber,
    String ocrName,
    String ocrAddress,
    String biometricTemplate,
    Integer livenessScore,
    String amlStatus,
    Boolean biometricMatch,
    Integer age,
    Instant submittedAt,
    Instant verifiedAt,
    String rejectionReason
) {
    public static KycVerification create(UUID agentId, String mykadNumber) {
        return new KycVerification(
            UUID.randomUUID(),
            agentId,
            KycStatus.IN_PROGRESS,
            mykadNumber,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public KycVerification withOcrData(String ocrName, String ocrAddress, String biometricTemplate) {
        return new KycVerification(
            id, agentId, status, mykadNumber, ocrName, ocrAddress,
            biometricTemplate, livenessScore, amlStatus, biometricMatch, age,
            submittedAt, verifiedAt, rejectionReason
        );
    }

    public KycVerification verify(Integer livenessScore, String amlStatus, Boolean biometricMatch, Integer age) {
        return new KycVerification(
            id, agentId, KycStatus.VERIFIED, mykadNumber, ocrName, ocrAddress,
            biometricTemplate, livenessScore, amlStatus, biometricMatch, age,
            submittedAt, Instant.now(), null
        );
    }

    public KycVerification reject(String reason) {
        return new KycVerification(
            id, agentId, KycStatus.REJECTED, mykadNumber, ocrName, ocrAddress,
            biometricTemplate, livenessScore, amlStatus, biometricMatch, age,
            submittedAt, verifiedAt, reason
        );
    }

    public boolean isAutoApprovalEligible() {
        return biometricMatch != null 
            && biometricMatch == true 
            && "CLEAN".equals(amlStatus) 
            && age != null 
            && age >= 18;
    }
}