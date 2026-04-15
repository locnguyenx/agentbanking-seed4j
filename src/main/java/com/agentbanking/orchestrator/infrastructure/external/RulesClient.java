package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.ValidationResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
  name = "rules-service",
  url = "${agentbanking.services.rules:http://rules-service:8082}",
  fallback = RulesClientFallback.class
)
public interface RulesClient {

  @PostMapping("/api/rules/validate")
  ValidationResultDTO validateTransaction(@RequestBody Object request);
}
