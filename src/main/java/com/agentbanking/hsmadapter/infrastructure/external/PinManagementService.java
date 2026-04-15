package com.agentbanking.hsmadapter.infrastructure.external;

import com.agentbanking.hsmadapter.domain.model.PinTranslationResult;
import com.agentbanking.hsmadapter.domain.model.PinVerificationResult;
import com.agentbanking.hsmadapter.infrastructure.external.HsmGatewayPort;
import com.agentbanking.hsmadapter.infrastructure.external.dto.HsmCommandRequest;
import com.agentbanking.hsmadapter.infrastructure.external.dto.HsmCommandResponse;
import java.time.Instant;

public class PinManagementService {

    private final HsmGatewayPort hsmGateway;

    public PinManagementService(HsmGatewayPort hsmGateway) {
        this.hsmGateway = hsmGateway;
    }

    public PinTranslationResult translatePin(String traceId, String encryptedPinBlock, String accountNumber) {
        try {
            HsmCommandRequest request = new HsmCommandRequest("TRANSLATE", encryptedPinBlock, accountNumber, traceId);
            HsmCommandResponse response = hsmGateway.translatePin(traceId, request);
            return mapToTranslationResult(response);
        } catch (Exception e) {
            return new PinTranslationResult(false, null, HsmErrorMapper.mapToInternal("HSM_ERROR"));
        }
    }

    public PinVerificationResult verifyPin(String traceId, String pinBlock, String accountNumber) {
        try {
            HsmCommandRequest request = new HsmCommandRequest("VERIFY", pinBlock, accountNumber, traceId);
            HsmCommandResponse response = hsmGateway.verifyPin(traceId, request);
            return new PinVerificationResult(
                "HSM000".equals(response.responseCode()),
                response.remainingAttempts(),
                HsmErrorMapper.mapToInternal(response.responseCode())
            );
        } catch (Exception e) {
            return new PinVerificationResult(false, 0, HsmErrorMapper.mapToInternal("HSM_ERROR"));
        }
    }

    public String generatePinBlock(String traceId, String pin, String accountNumber) {
        try {
            HsmCommandRequest request = new HsmCommandRequest("GENERATE", pin, accountNumber, traceId);
            HsmCommandResponse response = hsmGateway.generatePinBlock(traceId, request);
            return response.pinBlock();
        } catch (Exception e) {
            throw new RuntimeException("ERR_HSM_001: Failed to generate PIN block");
        }
    }

    private PinTranslationResult mapToTranslationResult(HsmCommandResponse response) {
        String internalCode = HsmErrorMapper.mapToInternal(response.responseCode());
        boolean success = "HSM000".equals(internalCode);
        return new PinTranslationResult(success, response.data(), internalCode);
    }
}