package com.agentbanking.onboarding.infrastructure.primary;

import com.agentbanking.onboarding.application.dto.AgentRegistrationRequest;
import com.agentbanking.onboarding.application.dto.AgentRegistrationResponse;
import com.agentbanking.onboarding.domain.port.in.AgentRegistrationUseCase;
import com.agentbanking.onboarding.domain.port.in.KYCVerificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
class OnboardingController {

  private final AgentRegistrationUseCase registrationUseCase;
  private final KYCVerificationUseCase kycVerificationUseCase;

  public OnboardingController(AgentRegistrationUseCase registrationUseCase,
                               KYCVerificationUseCase kycVerificationUseCase) {
    this.registrationUseCase = registrationUseCase;
    this.kycVerificationUseCase = kycVerificationUseCase;
  }

  @PostMapping("/register")
  public ResponseEntity<AgentRegistrationResponse> register(
      @Validated @RequestBody AgentRegistrationRequest request) {
    var domainResponse = registrationUseCase.registerAgent(request.toDomain());
    return ResponseEntity.ok(AgentRegistrationResponse.fromDomain(domainResponse));
  }

  @GetMapping("/agent/{agentId}")
  public ResponseEntity<AgentRegistrationResponse> getAgent(@PathVariable UUID agentId) {
    var domainResponse = registrationUseCase.getAgentById(agentId);
    return ResponseEntity.ok(AgentRegistrationResponse.fromDomain(domainResponse));
  }

  @PostMapping("/agent/{agentId}/kyc")
  public ResponseEntity<Boolean> submitKYC(
      @PathVariable UUID agentId,
      @RequestBody java.util.Map<String, String> request) {
    boolean result = kycVerificationUseCase.submitKYC(
      agentId,
      request.get("mykadNumber"),
      request.get("phoneNumber")
    );
    return ResponseEntity.ok(result);
  }
}