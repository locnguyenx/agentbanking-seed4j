package com.agentbanking.orchestrator.workflow.impl;

import com.agentbanking.orchestrator.activity.FloatActivity;
import com.agentbanking.orchestrator.activity.SwitchActivity;
import com.agentbanking.orchestrator.activity.TransactionActivity;
import com.agentbanking.orchestrator.infrastructure.activity.NotificationActivity;
import com.agentbanking.orchestrator.workflow.CashDepositWorkflow;
import com.agentbanking.orchestrator.workflow.dto.CashDepositWorkflowInput;
import com.agentbanking.orchestrator.workflow.dto.CashDepositWorkflowResult;
import com.agentbanking.shared.transaction.domain.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashDepositWorkflowImpl implements CashDepositWorkflow {

  private static final Logger log = LoggerFactory.getLogger(CashDepositWorkflowImpl.class);

  private final FloatActivity floatActivity;
  private final SwitchActivity switchActivity;
  private final TransactionActivity transactionActivity;
  private final NotificationActivity notificationActivity;

  private String creditId;
  private String stan;
  private boolean isReversed = false;

  public CashDepositWorkflowImpl(
      FloatActivity floatActivity,
      SwitchActivity switchActivity,
      TransactionActivity transactionActivity,
      NotificationActivity notificationActivity) {
    this.floatActivity = floatActivity;
    this.switchActivity = switchActivity;
    this.transactionActivity = transactionActivity;
    this.notificationActivity = notificationActivity;
  }

  @Override
  public CashDepositWorkflowResult execute(CashDepositWorkflowInput input) {
    log.info("Starting cash deposit workflow for saga: {}", input.sagaId());

    try {
      transactionActivity.validateTransaction(input.sagaId());

      stan = switchActivity.sendDeposit(
        input.agentId().value(),
        input.customerAccountId(),
        input.amount().amount().toString(),
        String.valueOf(System.currentTimeMillis())
      );

      floatActivity.creditAgentFloat(
        input.agentId().value(),
        input.amount().amount().toString(),
        "CREDIT-" + System.currentTimeMillis()
      );
      creditId = "CREDIT-" + System.currentTimeMillis();

      if (input.customerPhone() != null && !input.customerPhone().isBlank()) {
        notificationActivity.sendDepositSuccessNotification(
          input.customerPhone(),
          input.amount().amount().toString(),
          stan
        );
      }

      log.info("Cash deposit completed successfully for saga: {}", input.sagaId());

      return new CashDepositWorkflowResult(
        input.sagaId(),
        TransactionStatus.COMPLETED,
        input.amount(),
        null,
        null,
        stan
      );

    } catch (Exception e) {
      log.error("Cash deposit failed for saga: {}, error: {}", input.sagaId(), e.getMessage());

      compensate(input);

      return new CashDepositWorkflowResult(
        input.sagaId(),
        TransactionStatus.FAILED,
        input.amount(),
        "ERR_BIZ_001",
        e.getMessage(),
        null
      );
    }
  }

  private void compensate(CashDepositWorkflowInput input) {
    if (isReversed) {
      log.info("Already compensated, skipping for saga: {}", input.sagaId());
      return;
    }
    isReversed = true;

    log.info("Running compensation for cash deposit saga: {}", input.sagaId());

    try {
      if (creditId != null) {
        log.info("Reversing float credit: creditId={}", creditId);
        transactionActivity.compensateFloatDebit(
          input.agentId().value(),
          input.amount().amount().toString(),
          creditId
        );
      }

      if (input.customerPhone() != null && !input.customerPhone().isBlank()) {
        notificationActivity.sendReversalNotification(
          input.customerPhone(),
          input.amount().amount().toString(),
          "Transaction failed"
        );
      }

      if (stan != null) {
        switchActivity.sendReversal(stan, "Compensation triggered");
      }

    } catch (Exception e) {
      log.error("Compensation failed for saga: {}", e.getMessage());
    }
  }
}