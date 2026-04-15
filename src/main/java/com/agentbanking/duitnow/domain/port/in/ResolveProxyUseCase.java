package com.agentbanking.duitnow.domain.port.in;

import com.agentbanking.duitnow.domain.model.ProxyResolutionResult;
import com.agentbanking.duitnow.domain.model.ProxyType;

public interface ResolveProxyUseCase {
  ProxyResolutionResult resolve(ProxyType proxyType, String proxyValue);
}