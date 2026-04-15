package com.agentbanking.billeradapter.infrastructure.primary;

import com.agentbanking.billeradapter.application.dto.BillerResponseDTO;
import com.agentbanking.billeradapter.domain.port.in.BillerAdapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/biller-adapter")
class BillerAdapterController {

  private final BillerAdapterService billerAdapterService;

  public BillerAdapterController(BillerAdapterService billerAdapterService) {
    this.billerAdapterService = billerAdapterService;
  }

  @PostMapping("/pay")
  public ResponseEntity<BillerResponseDTO> payBill(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(BillerResponseDTO.fromDomain(billerAdapterService.payBill(
      request.get("billerCode"),
      request.get("referenceNumber"),
      request.get("amount"),
      request.get("customerAccount")
    )));
  }

  @GetMapping("/status/{billerCode}/{referenceNumber}")
  public ResponseEntity<BillerResponseDTO> queryStatus(
      @PathVariable String billerCode,
      @PathVariable String referenceNumber) {
    return ResponseEntity.ok(BillerResponseDTO.fromDomain(billerAdapterService.queryBillStatus(billerCode, referenceNumber)));
  }

  @PostMapping("/reverse")
  public ResponseEntity<BillerResponseDTO> reverse(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(BillerResponseDTO.fromDomain(billerAdapterService.reverseBillPayment(
      request.get("originalReference"),
      request.get("reason")
    )));
  }
}
