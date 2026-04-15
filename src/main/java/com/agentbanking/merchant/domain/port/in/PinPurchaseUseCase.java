package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface PinPurchaseUseCase {
  MerchantTransaction purchaseWithPin(String agentId, String merchantId, 
      String encryptedPin, String cardData, BigDecimal amount, String invoiceNumber);
}