package com.agentbanking.onboarding.domain.model;

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
) {}