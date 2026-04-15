package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.BalanceResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.CreditResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.DebitResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.LockResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
  name = "float-service",
  url = "${agentbanking.services.float:http://float-service:8083}",
  fallback = FloatClientFallback.class
)
public interface FloatClient {

  @PostMapping("/api/float/{agentId}/lock")
  LockResultDTO reserveFloat(@PathVariable String agentId, @RequestBody Object request);

  @PostMapping("/api/float/{agentId}/debit")
  DebitResultDTO debit(@PathVariable String agentId, @RequestBody Object request);

  @PostMapping("/api/float/{agentId}/credit")
  CreditResultDTO credit(@PathVariable String agentId, @RequestBody Object request);

  @GetMapping("/api/float/{agentId}/balance")
  BalanceResultDTO getBalance(@PathVariable String agentId);

  @DeleteMapping("/api/float/lock/{lockId}")
  void releaseLock(@PathVariable String lockId);
}
