package com.agentbanking.prepaid.domain.port.in;

import com.agentbanking.prepaid.domain.port.out.TopUpResult;
import java.math.BigDecimal;

public interface TopUpUseCase {
  TopUpResult topUp(String providerCode, String phoneNumber, BigDecimal amount, String idempotencyKey);
}