package com.agentbanking.prepaid.domain.service;

import com.agentbanking.prepaid.domain.model.TopUpProvider;
import com.agentbanking.prepaid.domain.port.in.TopUpUseCase;
import com.agentbanking.prepaid.domain.port.out.TopUpGatewayPort;
import com.agentbanking.prepaid.domain.port.out.TopUpProviderRegistryPort;
import com.agentbanking.prepaid.domain.port.out.TopUpResult;

import java.math.BigDecimal;

public class PrepaidTopUpService implements TopUpUseCase {

    private final TopUpGatewayPort topUpGatewayPort;
    private final TopUpProviderRegistryPort providerRegistryPort;
    private final PhoneValidationService phoneValidationService;

    public PrepaidTopUpService(
            TopUpGatewayPort topUpGatewayPort,
            TopUpProviderRegistryPort providerRegistryPort,
            PhoneValidationService phoneValidationService) {
        this.topUpGatewayPort = topUpGatewayPort;
        this.providerRegistryPort = providerRegistryPort;
        this.phoneValidationService = phoneValidationService;
    }

    @Override
    public TopUpResult topUp(String providerCode, String phoneNumber, BigDecimal amount, String idempotencyKey) {
        var provider = providerRegistryPort.findByCode(providerCode)
            .orElseThrow(() -> new IllegalArgumentException("ERR_VAL_015: Unknown provider code"));
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ERR_VAL_004: Amount must be positive");
        }
        
        if (amount.compareTo(new BigDecimal("300")) > 0) {
            throw new IllegalArgumentException("ERR_VAL_016: Amount exceeds maximum limit");
        }

        var validationResult = phoneValidationService.validatePhone(providerCode, phoneNumber);
        if (!validationResult.isValid()) {
            return new TopUpResult(false, null, null, "ERR_VAL_006: " + validationResult.getErrorMessage());
        }

        return topUpGatewayPort.topUp(providerCode, validationResult.getNormalizedPhone(), amount);
    }
}