package com.agentbanking.cbsadapter.infrastructure.primary;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.domain.port.in.CbsAdapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cbs-adapter")
class CbsAdapterController {

  private final CbsAdapterService cbsAdapterService;

  public CbsAdapterController(CbsAdapterService cbsAdapterService) {
    this.cbsAdapterService = cbsAdapterService;
  }

  @GetMapping("/balance/{accountId}")
  public ResponseEntity<CbsResponse> checkBalance(@PathVariable String accountId) {
    return ResponseEntity.ok(cbsAdapterService.checkBalance(accountId));
  }

  @PostMapping("/debit")
  public ResponseEntity<CbsResponse> debit(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(cbsAdapterService.debitAccount(
      request.get("accountId"),
      request.get("amount"),
      request.get("reference")
    ));
  }

  @PostMapping("/credit")
  public ResponseEntity<CbsResponse> credit(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(cbsAdapterService.creditAccount(
      request.get("accountId"),
      request.get("amount"),
      request.get("reference")
    ));
  }
}
