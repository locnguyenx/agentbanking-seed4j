package com.agentbanking.merchant.domain.service;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.in.*;
import com.agentbanking.merchant.domain.port.out.CardGatewayPort;
import com.agentbanking.merchant.domain.port.out.MerchantRegistryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MerchantService implements RetailSaleUseCase, PinPurchaseUseCase, 
    CalculateMdrUseCase, CashBackUseCase {

  private final CardGatewayPort cardGatewayPort;
  private final MerchantRegistryPort merchantRegistryPort;

  public MerchantService(CardGatewayPort cardGatewayPort, MerchantRegistryPort merchantRegistryPort) {
    this.cardGatewayPort = cardGatewayPort;
    this.merchantRegistryPort = merchantRegistryPort;
  }

  @Override
  public MerchantTransaction sale(String agentId, String merchantId, String cardData, 
      BigDecimal amount, String invoiceNumber) {
    
    MerchantCategory category = merchantRegistryPort.getMerchantCategory(merchantId)
      .orElseThrow(() -> new IllegalArgumentException("Unknown merchant: " + merchantId));
    
    MdrResult mdrResult = calculate(category, CardType.MYDEBIT, amount)
      .orElseThrow(() -> new IllegalArgumentException("No MDR config for category: " + category));
    
    BigDecimal mdrAmount = mdrResult.totalMdr();
    BigDecimal totalAmount = amount.add(mdrAmount);
    
    MerchantTransaction tx = cardGatewayPort.authorize(cardData, totalAmount);
    
    return new MerchantTransaction(
      tx.id(), agentId, UUID.randomUUID(), merchantId, CardType.MYDEBIT,
      maskCard(cardData), amount, mdrAmount, totalAmount, BigDecimal.ZERO,
      MerchantStatus.AUTHORIZED, tx.authorizationCode(), Instant.now(), null,
      invoiceNumber, null
    );
  }

  @Override
  public MerchantTransaction purchaseWithPin(String agentId, String merchantId, 
      String encryptedPin, String cardData, BigDecimal amount, String invoiceNumber) {
    return sale(agentId, merchantId, cardData, amount, invoiceNumber);
  }

  @Override
  public Optional<MdrResult> calculate(MerchantCategory category, CardType cardType, BigDecimal amount) {
    return merchantRegistryPort.getMdrConfig(category, cardType)
      .map(config -> {
        BigDecimal percentageAmt = amount.multiply(config.mdrPercentage())
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal fixedAmt = config.mdrFixed() != null ? config.mdrFixed() : BigDecimal.ZERO;
        return new MdrResult(config.mdrPercentage(), fixedAmt, percentageAmt.add(fixedAmt));
      });
  }

  @Override
  public MerchantTransaction processWithCashBack(String agentId, String merchantId, 
      String cardData, BigDecimal purchaseAmount, BigDecimal cashBackAmount, String invoiceNumber) {
    
    MerchantCategory category = merchantRegistryPort.getMerchantCategory(merchantId)
      .orElseThrow(() -> new IllegalArgumentException("Unknown merchant: " + merchantId));
    
    MdrResult mdrResult = calculate(category, CardType.MYDEBIT, purchaseAmount)
      .orElseThrow(() -> new IllegalArgumentException("No MDR config"));
    
    BigDecimal mdrAmount = mdrResult.totalMdr();
    BigDecimal totalAmount = purchaseAmount.add(mdrAmount).add(cashBackAmount);
    
    MerchantTransaction tx = cardGatewayPort.authorize(cardData, totalAmount);
    
    return new MerchantTransaction(
      tx.id(), agentId, UUID.randomUUID(), merchantId, CardType.MYDEBIT,
      maskCard(cardData), purchaseAmount, mdrAmount, totalAmount, cashBackAmount,
      MerchantStatus.AUTHORIZED, tx.authorizationCode(), Instant.now(), null,
      invoiceNumber, null
    );
  }

  private String maskCard(String cardData) {
    if (cardData == null || cardData.length() < 13) return "************";
    return cardData.substring(0, 6) + "******" + cardData.substring(cardData.length() - 4);
  }
}