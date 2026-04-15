package com.agentbanking.orchestrator.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TransactionActivity {

  @ActivityMethod(name = "validateTransaction")
  void validateTransaction(String input);

  @ActivityMethod(name = "lockAgentFloat")
  String lockAgentFloat(String agentId, String amount, String sagaId);

  @ActivityMethod(name = "debitAgentFloat")
  void debitAgentFloat(String agentId, String amount, String lockId);

  @ActivityMethod(name = "creditCustomer")
  void creditCustomer(String accountId, String amount);

  @ActivityMethod(name = "releaseFloatLock")
  void releaseFloatLock(String lockId);

  @ActivityMethod(name = "compensateFloatDebit")
  void compensateFloatDebit(String agentId, String amount, String debitId);
}