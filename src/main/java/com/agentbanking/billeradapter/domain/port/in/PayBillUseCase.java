package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import java.math.BigDecimal;

public interface PayBillUseCase {
  PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount, String idempotencyKey);
}