package com.agentbanking.orchestrator.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransactionWorkflow {

  @WorkflowMethod
  String execute(String input);

  @QueryMethod
  String getStatus();

  @SignalMethod
  void cancel();

  @SignalMethod
  void pause(String reason);
}
