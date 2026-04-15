package com.agentbanking.billeradapter.application.service;

import com.agentbanking.billeradapter.application.dto.*;
import com.agentbanking.billeradapter.domain.port.in.GetBillUseCase;
import com.agentbanking.billeradapter.domain.port.in.PayBillUseCase;
import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillerApplicationService {

  private static final Logger log = LoggerFactory.getLogger(BillerApplicationService.class);

  private final GetBillUseCase getBillUseCase;
  private final PayBillUseCase payBillUseCase;

  public BillerApplicationService(GetBillUseCase getBillUseCase, PayBillUseCase payBillUseCase) {
    this.getBillUseCase = getBillUseCase;
    this.payBillUseCase = payBillUseCase;
  }

  public BillInquiryResponse inquireBill(BillInquiryRequest request) {
    try {
      BillDetails details = getBillUseCase.getBillDetails(
          request.billerCode(), request.ref1(), request.ref2());
      return BillInquiryResponse.success(details.billerCode(), details.ref1(),
          details.customerName(), details.outstandingAmount(), details.billDescription());
    } catch (Exception e) {
      log.error("Bill inquiry failed: {}", e.getMessage());
      return BillInquiryResponse.failure("ERR_EXT_201", e.getMessage());
    }
  }

  public BillPaymentResponse payBill(BillPaymentRequest request) {
    try {
      PaymentResult result = payBillUseCase.payBill(
          request.billerCode(), request.ref1(), request.ref2(),
          request.amount(), request.idempotencyKey());

      if (result.success()) {
        return BillPaymentResponse.success(result.transactionId(), result.confirmationNumber(), request.amount());
      } else {
        log.warn("Bill payment failed: errorCode={}", result.errorCode());
        return BillPaymentResponse.failure(
            result.errorCode() != null ? result.errorCode() : "ERR_EXT_202", "Payment failed");
      }
    } catch (Exception e) {
      log.error("Bill payment error: {}", e.getMessage());
      return BillPaymentResponse.failure("ERR_EXT_201", e.getMessage());
    }
  }
}