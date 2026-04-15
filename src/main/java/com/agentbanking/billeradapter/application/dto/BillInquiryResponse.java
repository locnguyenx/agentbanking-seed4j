package com.agentbanking.billeradapter.application.dto;

import java.math.BigDecimal;

public record BillInquiryResponse(
    String code,
    String billerCode,
    String ref1,
    String customerName,
    BigDecimal outstandingAmount,
    String billDescription,
    String message
) {
  public static BillInquiryResponse success(String billerCode, String ref1,
      String customerName, BigDecimal outstandingAmount, String billDescription) {
    return new BillInquiryResponse("SUCCESS", billerCode, ref1, customerName,
        outstandingAmount, billDescription, "Bill inquiry successful");
  }

  public static BillInquiryResponse failure(String errorCode, String message) {
    return new BillInquiryResponse(errorCode, null, null, null, null, null, message);
  }
}