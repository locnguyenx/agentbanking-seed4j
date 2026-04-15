package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
  name = "onboarding-service",
  url = "${agentbanking.services.onboarding:http://onboarding-service:8081}",
  fallback = OnboardingClientFallback.class
)
public interface OnboardingClient {

  @GetMapping("/api/onboarding/agent/{agentId}")
  Object getAgent(@PathVariable String agentId);
}
