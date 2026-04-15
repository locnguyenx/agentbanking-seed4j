package com.agentbanking.onboarding.domain.model;

import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.shared.onboarding.domain.AgentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentRegistrationResponse(
  UUID agentId,
  String agentCode,
  String businessName,
  AgentType agentType,
  AgentStatus status,
  BigDecimal maxFloatLimit,
  Instant registeredAt,
  Instant approvedAt
) {}