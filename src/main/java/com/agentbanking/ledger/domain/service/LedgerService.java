package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LedgerService {

  private final LedgerRepository ledgerRepository;

  public LedgerService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public void recordTransaction(String transactionId, Money amount, 
                                  String debitAccount, String creditAccount,
                                  String description) {
    LedgerEntry debitEntry = new LedgerEntry(
      UUID.randomUUID(),
      transactionId,
      debitAccount,
      AccountType.ASSET,
      EntryType.DEBIT,
      amount,
      description,
      Instant.now()
    );
    
    LedgerEntry creditEntry = new LedgerEntry(
      UUID.randomUUID(),
      transactionId,
      creditAccount,
      AccountType.ASSET,
      EntryType.CREDIT,
      amount,
      description,
      Instant.now()
    );
    
    ledgerRepository.createEntry(debitEntry);
    ledgerRepository.createEntry(creditEntry);
  }

  public List<LedgerEntry> getEntriesForTransaction(String transactionId) {
    return ledgerRepository.findByTransactionId(transactionId);
  }
}