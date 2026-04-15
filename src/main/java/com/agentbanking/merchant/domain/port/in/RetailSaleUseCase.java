package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface RetailSaleUseCase {
  MerchantTransaction sale(String agentId, String merchantId, String cardData, 
      BigDecimal amount, String invoiceNumber);
}