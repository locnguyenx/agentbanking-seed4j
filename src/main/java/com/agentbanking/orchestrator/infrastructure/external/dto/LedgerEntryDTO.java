package com.agentbanking.orchestrator.infrastructure.external.dto;

import java.util.List;
import java.util.Map;

public record LedgerEntryDTO(
  String id,
  String transactionId,
  String accountId,
  String entryType,
  String amount,
  String description,
  String createdAt
) {}
