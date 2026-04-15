package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.infrastructure.external.dto.CreditResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.DebitResultDTO;
import com.agentbanking.orchestrator.infrastructure.external.dto.LedgerEntryDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class LedgerClientFallback implements LedgerClient {

  @Override
  public List<LedgerEntryDTO> getEntries(String transactionId) {
    return List.of();
  }

  @Override
  public DebitResultDTO debit(String accountId, BigDecimal amount, String reference) {
    return new DebitResultDTO(false, null, "ERR_EXT_102: Ledger service unavailable");
  }

  @Override
  public CreditResultDTO credit(String accountId, BigDecimal amount, String reference) {
    return new CreditResultDTO(false, null, "ERR_EXT_102: Ledger service unavailable");
  }
}
