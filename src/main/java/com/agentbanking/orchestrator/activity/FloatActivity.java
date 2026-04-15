package com.agentbanking.orchestrator.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FloatActivity {

  @ActivityMethod(name = "lockAgentFloat")
  String lockAgentFloat(String agentId, String amount, String sagaId);

  @ActivityMethod(name = "debitAgentFloat")
  void debitAgentFloat(String agentId, String amount, String lockId);

  @ActivityMethod(name = "creditAgentFloat")
  void creditAgentFloat(String agentId, String amount, String lockId);

  @ActivityMethod(name = "releaseFloatLock")
  void releaseFloatLock(String lockId);
}