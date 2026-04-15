package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.ValidationResultDTO;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RulesClientFallback implements RulesClient {

  @Override
  public ValidationResultDTO validateTransaction(Object request) {
    return new ValidationResultDTO(false, List.of("Rules service unavailable"), null);
  }
}
