package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface CashBackUseCase {
  MerchantTransaction processWithCashBack(String agentId, String merchantId, 
      String cardData, BigDecimal purchaseAmount, BigDecimal cashBackAmount, String invoiceNumber);
}