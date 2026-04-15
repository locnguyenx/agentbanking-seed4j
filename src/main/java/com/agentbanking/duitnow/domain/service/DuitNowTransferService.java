package com.agentbanking.duitnow.domain.service;

import com.agentbanking.duitnow.domain.model.*;
import com.agentbanking.duitnow.domain.port.in.ResolveProxyUseCase;
import com.agentbanking.duitnow.domain.port.in.TransferMoneyUseCase;
import com.agentbanking.duitnow.domain.port.out.PayNetGatewayPort;
import com.agentbanking.duitnow.domain.port.out.ProxyRegistryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DuitNowTransferService implements TransferMoneyUseCase, ResolveProxyUseCase {

  private final ProxyRegistryPort proxyRegistryPort;
  private final PayNetGatewayPort payNetGatewayPort;

  public DuitNowTransferService(ProxyRegistryPort proxyRegistryPort, PayNetGatewayPort payNetGatewayPort) {
    this.proxyRegistryPort = proxyRegistryPort;
    this.payNetGatewayPort = payNetGatewayPort;
  }

  @Override
  public DuitNowTransaction transfer(UUID traceId, String proxyTypeStr, String proxyValue, 
      BigDecimal amount, String reference, String idempotencyKey) {
    
    ProxyType proxyType = ProxyType.valueOf(proxyTypeStr.toUpperCase());
    
    ProxyResolutionResult resolved = resolve(proxyType, proxyValue);
    
    DuitNowTransaction tx = new DuitNowTransaction(
      UUID.randomUUID(), idempotencyKey, null, traceId, proxyType, proxyValue,
      resolved.resolvedAccountNumber(), resolved.bankCode(), resolved.recipientName(),
      amount, reference, DuitNowStatus.PROCESSING, Instant.now(), null, null
    );
    
    return payNetGatewayPort.transfer(tx.id(), resolved.resolvedAccountNumber(), 
      resolved.bankCode(), amount, reference);
  }

  @Override
  public ProxyResolutionResult resolve(ProxyType proxyType, String proxyValue) {
    return proxyRegistryPort.resolve(proxyType, proxyValue)
      .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyValue));
  }
}