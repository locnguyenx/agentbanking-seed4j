package com.agentbanking.onboarding.application.dto;

import com.agentbanking.shared.onboarding.domain.AgentType;
import java.math.BigDecimal;

public record AgentRegistrationRequest(
  String businessName,
  String businessRegistrationNumber,
  String ownerName,
  String mykadNumber,
  String phoneNumber,
  String email,
  String address,
  AgentType agentType,
  BigDecimal initialFloat
) {
  public com.agentbanking.onboarding.domain.model.AgentRegistrationRequest toDomain() {
    return new com.agentbanking.onboarding.domain.model.AgentRegistrationRequest(
      businessName,
      businessRegistrationNumber,
      ownerName,
      mykadNumber,
      phoneNumber,
      email,
      address,
      agentType,
      initialFloat
    );
  }
}