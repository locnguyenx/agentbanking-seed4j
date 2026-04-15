package com.agentbanking.billeradapter.domain.model;

public record Biller(
  String billerCode,
  String name,
  BillType type,
  String apiEndpoint,
  boolean supportsInquiry,
  boolean supportsReversal
) {
  public boolean isValid() {
    return billerCode != null && !billerCode.isBlank() && apiEndpoint != null;
  }
}