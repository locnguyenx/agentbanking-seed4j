package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.dto.CashDepositWorkflowInput;
import com.agentbanking.orchestrator.workflow.dto.CashDepositWorkflowResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashDepositWorkflow {

  @WorkflowMethod
  CashDepositWorkflowResult execute(CashDepositWorkflowInput input);
}