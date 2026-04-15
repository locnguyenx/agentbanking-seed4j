package com.agentbanking.onboarding.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.BiometricPort;
import com.agentbanking.onboarding.domain.port.out.JpnPort;
import com.agentbanking.onboarding.domain.port.out.KycRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.shared.onboarding.domain.AgentType;
import java.math.BigDecimal;
import java.util.UUID;
import com.agentbanking.onboarding.domain.model.Agent;

@UnitTest
@DisplayName("OnboardingService")
class OnboardingServiceTest {

    private AgentRepository agentRepository;
    private KycRepository kycRepository;
    private JpnPort jpnPort;
    private BiometricPort biometricPort;
    private OnboardingService service;

    @BeforeEach
    void setUp() {
        agentRepository = Mockito.mock(AgentRepository.class);
        kycRepository = Mockito.mock(KycRepository.class);
        jpnPort = Mockito.mock(JpnPort.class);
        biometricPort = Mockito.mock(BiometricPort.class);
        service = new OnboardingService(agentRepository, kycRepository, jpnPort, biometricPort);
    }

    @Nested
    @DisplayName("registerAgent")
    class RegisterAgent {

        @Test
        void shouldRejectDuplicateMykad() {
            var request = new com.agentbanking.onboarding.domain.model.AgentRegistrationRequest(
                "Test Business", "REG-001", "John Doe", 
                "900101011234", "+60123456789", "test@test.com",
                "123 Main St", AgentType.STANDARD, new BigDecimal("50000")
            );
            
            Mockito.when(agentRepository.existsByMykadNumber("900101011234")).thenReturn(true);

            assertThatThrownBy(() -> service.registerAgent(request))
                .isInstanceOf(DuplicateMykadException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_BIZ_219");
        }

        @Test
        void shouldRegisterNewAgent() {
            var request = new com.agentbanking.onboarding.domain.model.AgentRegistrationRequest(
                "Test Business", "REG-001", "John Doe", 
                "900101011234", "+60123456789", "test@test.com",
                "123 Main St", AgentType.STANDARD, new BigDecimal("50000")
            );
            
            Mockito.when(agentRepository.existsByMykadNumber("900101011234")).thenReturn(false);
            Mockito.when(agentRepository.save(Mockito.any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = service.registerAgent(request);

            assertThat(response.agentId()).isNotNull();
            assertThat(response.agentCode()).startsWith("AGT-");
            assertThat(response.businessName()).isEqualTo("Test Business");
            assertThat(response.status()).isEqualTo(AgentStatus.PENDING_APPROVAL);
        }
    }

    @Nested
    @DisplayName("getAgentsPendingReview")
    class GetAgentsPendingReview {

        @Test
        void shouldReturnAgentsWithPendingApprovalStatus() {
            Agent mockAgent = new Agent(
                UUID.randomUUID(), "AGT-001", AgentType.STANDARD,
                AgentStatus.PENDING_APPROVAL, "Test", "REG001", "Owner",
                "900101011234", "+60123456789", "test@test.com", "Address",
                java.time.Instant.now(), null
            );
            
            Mockito.when(agentRepository.findByStatus(AgentStatus.PENDING_APPROVAL))
                .thenReturn(List.of(mockAgent));

            var result = service.getAgentsPendingReview();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).businessName()).isEqualTo("Test");
        }
    }
}