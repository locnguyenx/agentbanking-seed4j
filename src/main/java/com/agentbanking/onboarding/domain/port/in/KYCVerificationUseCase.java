package com.agentbanking.onboarding.domain.port.in;

import java.util.UUID;

public interface KYCVerificationUseCase {

  boolean submitKYC(UUID agentId, String mykadNumber, String phoneNumber);
  
  boolean verifyBiometric(UUID agentId, String biometricData);
}