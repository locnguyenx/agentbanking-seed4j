package com.agentbanking.duitnow.infrastructure.adapter;

import com.agentbanking.duitnow.domain.model.ProxyResolutionResult;
import com.agentbanking.duitnow.domain.model.ProxyType;
import com.agentbanking.duitnow.domain.port.out.ProxyRegistryPort;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MockProxyRegistryAdapter implements ProxyRegistryPort {

  private static final Map<String, ProxyResolutionResult> PROXY_REGISTRY = Map.of(
    "60123456789", new ProxyResolutionResult(UUID.randomUUID(), "1234567890", "MBB", "Ahmad Bin Abu", ProxyType.MOBILE_NUMBER, true),
    "890101101234", new ProxyResolutionResult(UUID.randomUUID(), "9876543210", "CIMB", "Siti Nurhaliza", ProxyType.NRIC, true),
    "20210315001K", new ProxyResolutionResult(UUID.randomUUID(), "5555666677", "Public Bank", "Kedai Runcit Sdn Bhd", ProxyType.BUSINESS_REGISTRATION_NUMBER, true)
  );

  @Override
  public Optional<ProxyResolutionResult> resolve(ProxyType proxyType, String proxyValue) {
    return Optional.ofNullable(PROXY_REGISTRY.get(proxyValue));
  }
}