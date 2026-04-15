package com.agentbanking.billeradapter.infrastructure.secondary;

import com.agentbanking.billeradapter.domain.model.BillerResponse;
import com.agentbanking.billeradapter.domain.port.in.BillerAdapterService;
import java.time.Instant;

public class BillerAdapterServiceImpl implements BillerAdapterService {

  @Override
  public BillerResponse payBill(String billerCode, String referenceNumber, String amount, String customerAccount) {
    return new BillerResponse(
      true,
      "00",
      "Payment successful",
      "BILL-" + System.currentTimeMillis(),
      referenceNumber,
      Instant.now()
    );
  }

  @Override
  public BillerResponse queryBillStatus(String billerCode, String referenceNumber) {
    return new BillerResponse(
      true,
      "00",
      "Bill status retrieved",
      null,
      referenceNumber,
      Instant.now()
    );
  }

  @Override
  public BillerResponse reverseBillPayment(String originalReference, String reason) {
    return new BillerResponse(
      true,
      "00",
      "Reversal successful",
      "REV-" + System.currentTimeMillis(),
      originalReference,
      Instant.now()
    );
  }
}
