package com.agentbanking.onboarding.application.dto;

import com.agentbanking.shared.onboarding.domain.AgentType;
import java.math.BigDecimal;

public record AgentRegistrationRequestDTO(
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