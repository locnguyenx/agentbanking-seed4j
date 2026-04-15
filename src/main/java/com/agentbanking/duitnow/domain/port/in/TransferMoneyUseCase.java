package com.agentbanking.duitnow.domain.port.in;

import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import java.math.BigDecimal;
import java.util.UUID;

public interface TransferMoneyUseCase {
  DuitNowTransaction transfer(UUID traceId, String proxyType, String proxyValue, BigDecimal amount, String reference, String idempotencyKey);
}