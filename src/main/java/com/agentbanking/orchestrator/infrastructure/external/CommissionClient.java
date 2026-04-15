package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.CommissionEntryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
  name = "commission-service",
  url = "${agentbanking.services.commission:http://commission-service:8086}",
  fallback = CommissionClientFallback.class
)
public interface CommissionClient {

  @PostMapping("/api/commission/calculate")
  CommissionEntryDTO calculate(@RequestBody Object request);
}
