package com.agentbanking.orchestrator.infrastructure.external.dto;

import java.util.List;
import java.util.Map;

public record ValidationResultDTO(
  boolean valid,
  List<String> errors,
  Map<String, Object> feeCalculation
) {}
