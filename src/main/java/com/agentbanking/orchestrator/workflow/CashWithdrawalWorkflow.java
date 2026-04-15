package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalWorkflowInput;
import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalWorkflowResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashWithdrawalWorkflow {

  @WorkflowMethod
  CashWithdrawalWorkflowResult execute(CashWithdrawalWorkflowInput input);
}
