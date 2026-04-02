package com.agentbanking.account.domain;

import static com.agentbanking.shared.useridentity.domain.UsersIdentitiesFixture.*;

import java.util.List;
import com.agentbanking.shared.authentication.domain.Role;

public final class AccountsFixture {

  private AccountsFixture() {}

  public static Account account() {
    return Account.builder()
      .username(username())
      .firstname(firstname())
      .lastname(lastname())
      .email(email())
      .roles(List.of(Role.ADMIN.key()))
      .build();
  }
}
