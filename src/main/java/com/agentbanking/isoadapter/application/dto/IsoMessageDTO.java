package com.agentbanking.isoadapter.application.dto;

import java.util.Map;

public record IsoMessageDTO(
  String mti,
  Map<String, String> fields
) {
  public static IsoMessageDTO fromDomain(com.agentbanking.isoadapter.domain.model.IsoMessage domain) {
    Map<String, String> convertedFields = domain.fields().entrySet().stream()
      .collect(java.util.stream.Collectors.toMap(
        e -> e.getKey().toString(),
        e -> e.getValue()
      ));
    return new IsoMessageDTO(domain.mti(), convertedFields);
  }
}