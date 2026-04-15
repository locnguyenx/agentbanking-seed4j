package com.agentbanking.hsmadapter.infrastructure.secondary;

import com.agentbanking.hsmadapter.domain.model.PinTranslationResult;
import com.agentbanking.hsmadapter.domain.model.PinVerificationResult;
import com.agentbanking.hsmadapter.domain.port.in.HsmAdapterService;

public class HsmAdapterServiceImpl implements HsmAdapterService {

  @Override
  public PinTranslationResult translatePin(String encryptedPinBlock, String accountNumber) {
    // In production, would call HSM for PIN translation
    // This is a stub - never log or store actual PINs
    return new PinTranslationResult(
      true,
      "TRANSLATED_PIN", // Never return actual PIN
      null
    );
  }

  @Override
  public PinVerificationResult verifyPin(String pinBlock, String accountNumber) {
    // In production, would call HSM for PIN verification
    return new PinVerificationResult(
      true,
      3,
      null
    );
  }

  @Override
  public String generatePinBlock(String pin, String accountNumber) {
    // In production, would call HSM to generate PIN block
    // This is a stub - never log or store actual PINs
    return "PIN_BLOCK_" + System.currentTimeMillis();
  }
}
