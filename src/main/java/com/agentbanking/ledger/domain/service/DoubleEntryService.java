package com.agentbanking.ledger.domain.service;

import com.agentbanking.shared.money.domain.Money;

public class DoubleEntryService {

  public boolean validateDoubleEntry(Money debitAmount, Money creditAmount) {
    return debitAmount.amount().compareTo(creditAmount.amount()) == 0;
  }
}