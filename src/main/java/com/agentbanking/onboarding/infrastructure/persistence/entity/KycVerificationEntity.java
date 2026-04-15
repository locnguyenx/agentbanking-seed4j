package com.agentbanking.onboarding.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.KycStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_verification")
public class KycVerificationEntity {
    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KycStatus status;

    @Column(name = "mykad_number", length = 12)
    private String mykadNumber;

    @Column(name = "ocr_name")
    private String ocrName;

    @Column(name = "ocr_address")
    private String ocrAddress;

    @Column(name = "biometric_template", columnDefinition = "TEXT")
    private String biometricTemplate;

    @Column(name = "liveness_score")
    private Integer livenessScore;

    @Column(name = "aml_status")
    private String amlStatus;

    @Column(name = "biometric_match")
    private Boolean biometricMatch;

    @Column(name = "age")
    private Integer age;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }
    public String getMykadNumber() { return mykadNumber; }
    public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
    public String getOcrName() { return ocrName; }
    public void setOcrName(String ocrName) { this.ocrName = ocrName; }
    public String getOcrAddress() { return ocrAddress; }
    public void setOcrAddress(String ocrAddress) { this.ocrAddress = ocrAddress; }
    public String getBiometricTemplate() { return biometricTemplate; }
    public void setBiometricTemplate(String biometricTemplate) { this.biometricTemplate = biometricTemplate; }
    public Integer getLivenessScore() { return livenessScore; }
    public void setLivenessScore(Integer livenessScore) { this.livenessScore = livenessScore; }
    public String getAmlStatus() { return amlStatus; }
    public void setAmlStatus(String amlStatus) { this.amlStatus = amlStatus; }
    public Boolean getBiometricMatch() { return biometricMatch; }
    public void setBiometricMatch(Boolean biometricMatch) { this.biometricMatch = biometricMatch; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}