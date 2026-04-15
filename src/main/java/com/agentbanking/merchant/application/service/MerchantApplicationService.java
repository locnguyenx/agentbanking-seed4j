package com.agentbanking.merchant.application.service;

import com.agentbanking.merchant.application.dto.RetailSaleRequest;
import com.agentbanking.merchant.application.dto.RetailSaleResponse;
import com.agentbanking.merchant.domain.model.MerchantTransaction;
import com.agentbanking.merchant.domain.port.in.CashBackUseCase;
import com.agentbanking.merchant.domain.port.in.PinPurchaseUseCase;
import com.agentbanking.merchant.domain.port.in.RetailSaleUseCase;

public class MerchantApplicationService {

  private final RetailSaleUseCase retailSaleUseCase;
  private final PinPurchaseUseCase pinPurchaseUseCase;
  private final CashBackUseCase cashBackUseCase;

  public MerchantApplicationService(
      RetailSaleUseCase retailSaleUseCase,
      PinPurchaseUseCase pinPurchaseUseCase,
      CashBackUseCase cashBackUseCase) {
    this.retailSaleUseCase = retailSaleUseCase;
    this.pinPurchaseUseCase = pinPurchaseUseCase;
    this.cashBackUseCase = cashBackUseCase;
  }

  public RetailSaleResponse processSale(RetailSaleRequest request, String agentId) {
    MerchantTransaction tx = retailSaleUseCase.sale(
      agentId,
      request.merchantId(),
      request.cardData(),
      request.amount(),
      request.invoiceNumber()
    );
    
    return toResponse(tx);
  }

  public RetailSaleResponse processPinPurchase(
      String merchantId, String encryptedPin, String cardData, 
      java.math.BigDecimal amount, String invoiceNumber, String agentId) {
    MerchantTransaction tx = pinPurchaseUseCase.purchaseWithPin(
      agentId, merchantId, encryptedPin, cardData, amount, invoiceNumber
    );
    
    return toResponse(tx);
  }

  public RetailSaleResponse processCashBack(
      String merchantId, String cardData, 
      java.math.BigDecimal purchaseAmount, java.math.BigDecimal cashBackAmount,
      String invoiceNumber, String agentId) {
    MerchantTransaction tx = cashBackUseCase.processWithCashBack(
      agentId, merchantId, cardData, purchaseAmount, cashBackAmount, invoiceNumber
    );
    
    return toResponse(tx);
  }

  private RetailSaleResponse toResponse(MerchantTransaction tx) {
    return new RetailSaleResponse(
      tx.id(), tx.status().name(), tx.authorizationCode(),
      tx.transactionAmount(), tx.mdrAmount(), tx.totalAmount(), 
      tx.cashBackAmount(), tx.capturedAt()
    );
  }
}