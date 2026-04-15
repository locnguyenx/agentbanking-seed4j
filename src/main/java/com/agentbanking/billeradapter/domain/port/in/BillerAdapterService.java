package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.model.BillerResponse;

public interface BillerAdapterService {

  BillerResponse payBill(String billerCode, String referenceNumber, String amount, String customerAccount);
  
  BillerResponse queryBillStatus(String billerCode, String referenceNumber);
  
  BillerResponse reverseBillPayment(String originalReference, String reason);
}
