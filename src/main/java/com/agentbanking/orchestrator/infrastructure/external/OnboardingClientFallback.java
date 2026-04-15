package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.stereotype.Component;

@Component
public class OnboardingClientFallback implements OnboardingClient {

  @Override
  public Object getAgent(String agentId) {
    return null;
  }
}
