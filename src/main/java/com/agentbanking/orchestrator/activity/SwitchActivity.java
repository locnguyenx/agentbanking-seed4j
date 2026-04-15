package com.agentbanking.orchestrator.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SwitchActivity {

  @ActivityMethod(name = "sendWithdrawal")
  String sendWithdrawal(String agentId, String customerAccountId, String amount, String stan);

  @ActivityMethod(name = "sendDeposit")
  String sendDeposit(String agentId, String customerAccountId, String amount, String stan);

  @ActivityMethod(name = "sendReversal")
  void sendReversal(String originalStan, String reason);
}