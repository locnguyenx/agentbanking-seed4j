package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.Agent;
import com.agentbanking.onboarding.domain.model.AgentRegistrationRequest;
import com.agentbanking.onboarding.domain.model.AgentRegistrationResponse;
import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.model.KycVerification;
import com.agentbanking.onboarding.domain.port.in.AgentRegistrationUseCase;
import com.agentbanking.onboarding.domain.port.in.KYCVerificationUseCase;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.BiometricMatchResult;
import com.agentbanking.onboarding.domain.port.out.BiometricPort;
import com.agentbanking.onboarding.domain.port.out.JpnPort;
import com.agentbanking.onboarding.domain.port.out.JpnVerificationResult;
import com.agentbanking.onboarding.domain.port.out.KycRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OnboardingService implements AgentRegistrationUseCase, KYCVerificationUseCase {

    private final AgentRepository agentRepository;
    private final KycRepository kycRepository;
    private final JpnPort jpnPort;
    private final BiometricPort biometricPort;

    public OnboardingService(
            AgentRepository agentRepository,
            KycRepository kycRepository,
            JpnPort jpnPort,
            BiometricPort biometricPort
    ) {
        this.agentRepository = agentRepository;
        this.kycRepository = kycRepository;
        this.jpnPort = jpnPort;
        this.biometricPort = biometricPort;
    }

    @Override
    public AgentRegistrationResponse registerAgent(AgentRegistrationRequest request) {
        if (agentRepository.existsByMykadNumber(request.mykadNumber())) {
            throw new DuplicateMykadException("ERR_BIZ_219");
        }

        Agent agent = Agent.register(
            request.businessName(),
            request.businessRegistrationNumber(),
            request.ownerName(),
            request.mykadNumber(),
            request.phoneNumber(),
            request.email(),
            request.address(),
            request.agentType()
        );

        Agent saved = agentRepository.save(agent);

        return new AgentRegistrationResponse(
            saved.id(),
            saved.agentCode(),
            saved.businessName(),
            saved.agentType(),
            saved.status(),
            request.initialFloat(),
            saved.registeredAt(),
            null
        );
    }

    @Override
    public AgentRegistrationResponse getAgentById(UUID agentId) {
        return agentRepository.findById(agentId)
            .map(agent -> new AgentRegistrationResponse(
                agent.id(),
                agent.agentCode(),
                agent.businessName(),
                agent.agentType(),
                agent.status(),
                null,
                agent.registeredAt(),
                agent.approvedAt()
            ))
            .orElse(null);
    }

    @Override
    public boolean submitKYC(UUID agentId, String mykadNumber, String phoneNumber) {
        if (mykadNumber == null || mykadNumber.length() != 12 || !mykadNumber.matches("\\d+")) {
            return false;
        }

        if (phoneNumber == null || !phoneNumber.matches("^\\+?60\\d{9,10}$")) {
            return false;
        }

        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            return false;
        }

        if (!jpnPort.isAvailable()) {
            return false;
        }

        JpnVerificationResult jpnResult = jpnPort.verifyMykad(mykadNumber);
        if (!jpnResult.success()) {
            return false;
        }

        KycVerification kycVerification = KycVerification.create(agentId, mykadNumber);
        kycVerification = kycVerification.withOcrData(
            jpnResult.extractedName(),
            jpnResult.extractedAddress(),
            null
        );
        kycRepository.save(kycVerification);

        return true;
    }

    @Override
    public boolean verifyBiometric(UUID agentId, String biometricData) {
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            return false;
        }

        KycVerification kycVerification = kycRepository.findByAgentId(agentId).orElse(null);
        if (kycVerification == null) {
            return false;
        }

        if (!biometricPort.isAvailable()) {
            return false;
        }

        BiometricMatchResult biometricResult = biometricPort.verifyThumbprint(biometricData);
        if (!biometricResult.match()) {
            kycRepository.save(kycVerification.verify(0, "CLEAN", false, null));
            return false;
        }

        JpnVerificationResult jpnResult = jpnPort.verifyMykad(agent.mykadNumber());
        
        KycVerification verifiedKyc = kycVerification.verify(
            biometricResult.confidence(),
            jpnResult.amlStatus(),
            true,
            jpnResult.age()
        );
        kycRepository.save(verifiedKyc);

        if (verifiedKyc.isAutoApprovalEligible()) {
            Agent approved = agent.completeKyc();
            agentRepository.save(approved);
        }

        return true;
    }

    public List<Agent> getAgentsPendingReview() {
        return agentRepository.findByStatus(AgentStatus.PENDING_APPROVAL);
    }

    public List<KycVerification> getKycPendingReview() {
        return kycRepository.findByStatus(KycStatus.PENDING_VERIFICATION);
    }
}