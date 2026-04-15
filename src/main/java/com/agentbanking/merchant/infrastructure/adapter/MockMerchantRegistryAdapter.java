package com.agentbanking.merchant.infrastructure.adapter;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.out.MerchantRegistryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MockMerchantRegistryAdapter implements MerchantRegistryPort {

  private static final Map<String, MerchantCategory> MERCHANTS = Map.of(
    "MERCH001", MerchantCategory.RETAIL,
    "MERCH002", MerchantCategory.FNB,
    "MERCH003", MerchantCategory.PETROL
  );

  private static final List<MdrConfig> MDR_CONFIGS = List.of(
    new MdrConfig(UUID.randomUUID(), MerchantCategory.RETAIL, CardType.VISA, 
      new BigDecimal("1.50"), new BigDecimal("0.20"), 
      new BigDecimal("1.00"), new BigDecimal("5000.00"), true),
    new MdrConfig(UUID.randomUUID(), MerchantCategory.RETAIL, CardType.MYDEBIT, 
      new BigDecimal("0.50"), new BigDecimal("0.10"), 
      new BigDecimal("1.00"), new BigDecimal("3000.00"), true),
    new MdrConfig(UUID.randomUUID(), MerchantCategory.FNB, CardType.VISA, 
      new BigDecimal("2.00"), new BigDecimal("0.30"), 
      new BigDecimal("1.00"), new BigDecimal("3000.00"), false)
  );

  @Override
  public Optional<String> getMerchantName(String merchantId) {
    return Optional.of("Test Merchant " + merchantId);
  }

  @Override
  public Optional<MerchantCategory> getMerchantCategory(String merchantId) {
    return Optional.ofNullable(MERCHANTS.get(merchantId));
  }

  @Override
  public List<MdrConfig> getMdrConfigs() {
    return MDR_CONFIGS;
  }

  @Override
  public Optional<MdrConfig> getMdrConfig(MerchantCategory category, CardType cardType) {
    return MDR_CONFIGS.stream()
      .filter(c -> c.category() == category && c.cardType() == cardType)
      .findFirst();
  }
}