package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.CommissionEntryDTO;
import org.springframework.stereotype.Component;

@Component
public class CommissionClientFallback implements CommissionClient {

  @Override
  public CommissionEntryDTO calculate(Object request) {
    return null;
  }
}
