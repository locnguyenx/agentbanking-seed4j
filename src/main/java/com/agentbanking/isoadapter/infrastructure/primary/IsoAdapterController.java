package com.agentbanking.isoadapter.infrastructure.primary;

import com.agentbanking.isoadapter.application.dto.IsoMessageDTO;
import com.agentbanking.isoadapter.domain.port.in.IsoAdapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/iso-adapter")
class IsoAdapterController {

  private final IsoAdapterService isoAdapterService;

  public IsoAdapterController(IsoAdapterService isoAdapterService) {
    this.isoAdapterService = isoAdapterService;
  }

  @PostMapping("/withdrawal")
  public ResponseEntity<IsoMessageDTO> buildWithdrawal(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(IsoMessageDTO.fromDomain(isoAdapterService.buildWithdrawalMessage(
      request.get("agentId"),
      request.get("customerAccount"),
      request.get("amount"),
      request.get("stan")
    )));
  }

  @PostMapping("/deposit")
  public ResponseEntity<IsoMessageDTO> buildDeposit(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(IsoMessageDTO.fromDomain(isoAdapterService.buildDepositMessage(
      request.get("agentId"),
      request.get("customerAccount"),
      request.get("amount"),
      request.get("stan")
    )));
  }

  @PostMapping("/reversal")
  public ResponseEntity<IsoMessageDTO> buildReversal(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(IsoMessageDTO.fromDomain(isoAdapterService.buildReversalMessage(
      request.get("originalStan"),
      request.get("reason")
    )));
  }
}
