package com.agentbanking.onboarding.application.dto;

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
) {
  public static AgentRegistrationResponse fromDomain(com.agentbanking.onboarding.domain.model.AgentRegistrationResponse domain) {
    return new AgentRegistrationResponse(
      domain.agentId(),
      domain.agentCode(),
      domain.businessName(),
      domain.agentType(),
      domain.status(),
      domain.maxFloatLimit(),
      domain.registeredAt(),
      domain.approvedAt()
    );
  }
}