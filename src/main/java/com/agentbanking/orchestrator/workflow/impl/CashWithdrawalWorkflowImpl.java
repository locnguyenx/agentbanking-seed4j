package com.agentbanking.orchestrator.workflow.impl;

import com.agentbanking.orchestrator.activity.FloatActivity;
import com.agentbanking.orchestrator.activity.SwitchActivity;
import com.agentbanking.orchestrator.activity.TransactionActivity;
import com.agentbanking.orchestrator.infrastructure.activity.NotificationActivity;
import com.agentbanking.orchestrator.workflow.CashWithdrawalWorkflow;
import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalWorkflowInput;
import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalWorkflowResult;
import com.agentbanking.shared.transaction.domain.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashWithdrawalWorkflowImpl implements CashWithdrawalWorkflow {

  private static final Logger log = LoggerFactory.getLogger(CashWithdrawalWorkflowImpl.class);

  private final FloatActivity floatActivity;
  private final SwitchActivity switchActivity;
  private final TransactionActivity transactionActivity;
  private final NotificationActivity notificationActivity;

  private String lockId;
  private String debitId;
  private String stan;
  private boolean isReversed = false;

  public CashWithdrawalWorkflowImpl(
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
  public CashWithdrawalWorkflowResult execute(CashWithdrawalWorkflowInput input) {
    log.info("Starting cash withdrawal workflow for saga: {}", input.sagaId());

    try {
      transactionActivity.validateTransaction(input.sagaId());

      lockId = floatActivity.lockAgentFloat(
        input.agentId().value(),
        input.amount().amount().toString(),
        input.sagaId()
      );

      floatActivity.debitAgentFloat(
        input.agentId().value(),
        input.amount().amount().toString(),
        lockId
      );
      debitId = "DEBIT-" + System.currentTimeMillis();

      stan = switchActivity.sendWithdrawal(
        input.agentId().value(),
        input.customerAccountId(),
        input.amount().amount().toString(),
        String.valueOf(System.currentTimeMillis())
      );

      floatActivity.releaseFloatLock(lockId);

      if (input.customerPhone() != null && !input.customerPhone().isBlank()) {
        notificationActivity.sendWithdrawalSuccessNotification(
          input.customerPhone(),
          input.amount().amount().toString(),
          stan
        );
      }

      log.info("Cash withdrawal completed successfully for saga: {}", input.sagaId());

      return new CashWithdrawalWorkflowResult(
        input.sagaId(),
        TransactionStatus.COMPLETED,
        input.amount(),
        null,
        null,
        stan
      );

    } catch (Exception e) {
      log.error("Cash withdrawal failed for saga: {}, error: {}", input.sagaId(), e.getMessage());

      compensate(input);

      return new CashWithdrawalWorkflowResult(
        input.sagaId(),
        TransactionStatus.FAILED,
        input.amount(),
        "ERR_BIZ_001",
        e.getMessage(),
        null
      );
    }
  }

  private void compensate(CashWithdrawalWorkflowInput input) {
    if (isReversed) {
      log.info("Already compensated, skipping for saga: {}", input.sagaId());
      return;
    }
    isReversed = true;

    log.info("Running compensation for saga: {}", input.sagaId());

    try {
      if (debitId != null && lockId != null) {
        log.info("Reversing float debit: debitId={}, lockId={}", debitId, lockId);
        transactionActivity.compensateFloatDebit(
          input.agentId().value(),
          input.amount().amount().toString(),
          debitId
        );
      }

      if (lockId != null) {
        floatActivity.releaseFloatLock(lockId);
        log.info("Released float lock: lockId={}", lockId);
      }

      if (input.customerPhone() != null && !input.customerPhone().isBlank()) {
        notificationActivity.sendReversalNotification(
          input.customerPhone(),
          input.amount().amount().toString(),
          "Transaction failed"
        );
      }

      switchActivity.sendReversal(stan != null ? stan : "N/A", "Compensation triggered");

    } catch (Exception e) {
      log.error("Compensation failed for saga: {}", e.getMessage());
    }
  }
}
