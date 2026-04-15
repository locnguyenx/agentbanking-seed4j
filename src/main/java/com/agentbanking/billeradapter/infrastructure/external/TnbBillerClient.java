package com.agentbanking.billeradapter.infrastructure.external;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TnbBillerClient {

  private static final Logger log = LoggerFactory.getLogger(TnbBillerClient.class);

  public BillDetails getBillDetails(String ref1, String ref2) {
    log.info("Calling TNB bill inquiry API for ref: {}", ref1);
    return new BillDetails("TNB", ref1, "CUSTOMER NAME", new BigDecimal("150.00"), "Electricity Bill - TNB");
  }

  public PaymentResult payBill(String ref1, String ref2, BigDecimal amount) {
    log.info("Calling TNB payment API for ref: {}, amount: {}", ref1, amount);
    return new PaymentResult(true,
        "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
        "CONF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null);
  }

  public ReversalResult reverseBill(String transactionId, String reason) {
    log.info("Calling TNB reversal API for transaction: {}", transactionId);
    return new ReversalResult(true,
        "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
  }
}