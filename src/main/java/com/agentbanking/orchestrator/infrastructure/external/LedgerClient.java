package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.DebitResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.CreditResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.LedgerEntryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(
  name = "ledger-service",
  url = "${agentbanking.services.ledger:http://ledger-service:8085}",
  fallback = LedgerClientFallback.class
)
public interface LedgerClient {

  @GetMapping("/api/ledger/transaction/{transactionId}/entries")
  List<LedgerEntryDTO> getEntries(@PathVariable String transactionId);

  @PostMapping("/api/ledger/debit")
  DebitResultDTO debit(@RequestParam String accountId, @RequestParam java.math.BigDecimal amount, @RequestParam String reference);

  @PostMapping("/api/ledger/credit")
  CreditResultDTO credit(@RequestParam String accountId, @RequestParam java.math.BigDecimal amount, @RequestParam String reference);
}
