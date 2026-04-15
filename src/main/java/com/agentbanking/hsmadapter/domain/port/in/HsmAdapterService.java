package com.agentbanking.hsmadapter.domain.port.in;

import com.agentbanking.hsmadapter.domain.model.PinTranslationResult;
import com.agentbanking.hsmadapter.domain.model.PinVerificationResult;

public interface HsmAdapterService {

  PinTranslationResult translatePin(String encryptedPinBlock, String accountNumber);
  
  PinVerificationResult verifyPin(String pinBlock, String accountNumber);
  
  String generatePinBlock(String pin, String accountNumber);
}
