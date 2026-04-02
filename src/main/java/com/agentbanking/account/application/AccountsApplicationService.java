package com.agentbanking.account.application;

import com.agentbanking.account.domain.Account;
import com.agentbanking.account.domain.AccountsRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AccountsApplicationService {

  private final AccountsRepository accounts;

  public AccountsApplicationService(AccountsRepository accounts) {
    this.accounts = accounts;
  }

  public Optional<Account> authenticatedUserAccount() {
    return accounts.authenticatedUserAccount();
  }
}
