package com.agentbanking.ledger.infrastructure.primary;

import com.agentbanking.ledger.application.dto.LedgerEntryDTO;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ledger")
class LedgerController {

  private final LedgerService ledgerService;

  public LedgerController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @GetMapping("/transaction/{transactionId}/entries")
  public ResponseEntity<List<LedgerEntryDTO>> getEntries(@PathVariable String transactionId) {
    return ResponseEntity.ok(
      ledgerService.getEntriesForTransaction(transactionId).stream()
        .map(LedgerEntryDTO::fromDomain)
        .toList()
    );
  }
}