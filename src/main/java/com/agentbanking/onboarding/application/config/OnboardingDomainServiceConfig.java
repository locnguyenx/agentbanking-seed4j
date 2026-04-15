package com.agentbanking.onboarding.application.config;

import com.agentbanking.onboarding.domain.port.in.AgentRegistrationUseCase;
import com.agentbanking.onboarding.domain.port.in.KYCVerificationUseCase;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.BiometricPort;
import com.agentbanking.onboarding.domain.port.out.JpnPort;
import com.agentbanking.onboarding.domain.port.out.KycRepository;
import com.agentbanking.onboarding.domain.service.OnboardingService;
import com.agentbanking.onboarding.infrastructure.persistence.adapter.AgentRepositoryAdapter;
import com.agentbanking.onboarding.infrastructure.persistence.adapter.KycRepositoryAdapter;
import com.agentbanking.onboarding.infrastructure.persistence.repository.AgentJpaRepository;
import com.agentbanking.onboarding.infrastructure.persistence.repository.KycJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnboardingDomainServiceConfig {

    @Bean
    public AgentRepository agentRepository(AgentJpaRepository agentJpaRepository) {
        return new AgentRepositoryAdapter(agentJpaRepository);
    }

    @Bean
    public KycRepository kycRepository(KycJpaRepository kycJpaRepository) {
        return new KycRepositoryAdapter(kycJpaRepository);
    }

    @Bean
    public OnboardingService onboardingService(
            AgentRepository agentRepository,
            KycRepository kycRepository,
            JpnPort jpnPort,
            BiometricPort biometricPort
    ) {
        return new OnboardingService(agentRepository, kycRepository, jpnPort, biometricPort);
    }

    @Bean
    public AgentRegistrationUseCase agentRegistrationUseCase(OnboardingService service) {
        return service;
    }

    @Bean
    public KYCVerificationUseCase kycVerificationUseCase(OnboardingService service) {
        return service;
    }
}