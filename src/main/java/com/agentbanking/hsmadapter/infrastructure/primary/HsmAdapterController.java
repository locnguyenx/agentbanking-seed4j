package com.agentbanking.hsmadapter.infrastructure.primary;

import com.agentbanking.hsmadapter.domain.model.PinTranslationResult;
import com.agentbanking.hsmadapter.domain.model.PinVerificationResult;
import com.agentbanking.hsmadapter.domain.port.in.HsmAdapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/hsm-adapter")
class HsmAdapterController {

  private final HsmAdapterService hsmAdapterService;

  public HsmAdapterController(HsmAdapterService hsmAdapterService) {
    this.hsmAdapterService = hsmAdapterService;
  }

  @PostMapping("/translate-pin")
  public ResponseEntity<PinTranslationResult> translatePin(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(hsmAdapterService.translatePin(
      request.get("encryptedPinBlock"),
      request.get("accountNumber")
    ));
  }

  @PostMapping("/verify-pin")
  public ResponseEntity<PinVerificationResult> verifyPin(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(hsmAdapterService.verifyPin(
      request.get("pinBlock"),
      request.get("accountNumber")
    ));
  }

  @PostMapping("/generate-pin-block")
  public ResponseEntity<Map<String, String>> generatePinBlock(
      @RequestBody Map<String, String> request) {
    String pinBlock = hsmAdapterService.generatePinBlock(
      request.get("pin"),
      request.get("accountNumber")
    );
    return ResponseEntity.ok(Map.of("pinBlock", pinBlock));
  }
}
