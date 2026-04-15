package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRegistrationRequest;
import com.agentbanking.onboarding.domain.model.AgentRegistrationResponse;
import java.util.UUID;

public interface AgentRegistrationUseCase {

  AgentRegistrationResponse registerAgent(AgentRegistrationRequest request);
  
  AgentRegistrationResponse getAgentById(UUID agentId);
}