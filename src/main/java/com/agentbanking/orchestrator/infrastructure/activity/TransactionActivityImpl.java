package com.agentbanking.orchestrator.infrastructure.activity;

import com.agentbanking.orchestrator.activity.TransactionActivity;
import com.agentbanking.orchestrator.infrastructure.external.LedgerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionActivityImpl implements TransactionActivity {

    private static final Logger log = LoggerFactory.getLogger(TransactionActivityImpl.class);

    private final LedgerClient ledgerClient;

    public TransactionActivityImpl(LedgerClient ledgerClient) {
        this.ledgerClient = ledgerClient;
    }

    @Override
    public void validateTransaction(String input) {
        log.info("Validating transaction: {}", input);
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("ERR_VAL_001: Transaction input is required");
        }
        log.info("Transaction validation passed");
    }

    @Override
    public String lockAgentFloat(String agentId, String amount, String sagaId) {
        log.info("Locking agent float: agentId={}, amount={}, sagaId={}", agentId, amount, sagaId);
        String lockId = "LOCK-" + System.currentTimeMillis();
        log.info("Float locked: lockId={}", lockId);
        return lockId;
    }

    @Override
    public void debitAgentFloat(String agentId, String amount, String lockId) {
        log.info("Debiting agent float: agentId={}, amount={}, lockId={}", agentId, amount, lockId);
        try {
            ledgerClient.debit(agentId, new BigDecimal(amount), lockId);
            log.info("Float debited successfully");
        } catch (Exception e) {
            log.error("Failed to debit float: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_003: Failed to debit float", e);
        }
    }

    @Override
    public void creditCustomer(String accountId, String amount) {
        log.info("Crediting customer: accountId={}, amount={}", accountId, amount);
        try {
            ledgerClient.credit(accountId, new BigDecimal(amount), "CREDIT-" + System.currentTimeMillis());
            log.info("Customer credited successfully");
        } catch (Exception e) {
            log.error("Failed to credit customer: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_006: Failed to credit customer", e);
        }
    }

    @Override
    public void releaseFloatLock(String lockId) {
        log.info("Releasing float lock: lockId={}", lockId);
        log.info("Float lock released");
    }

    @Override
    public void compensateFloatDebit(String agentId, String amount, String debitId) {
        log.info("Compensating float debit: agentId={}, amount={}, debitId={}", agentId, amount, debitId);
        try {
            ledgerClient.credit(agentId, new BigDecimal(amount), "COMP-" + System.currentTimeMillis());
            log.info("Float debit compensated successfully");
        } catch (Exception e) {
            log.error("Failed to compensate float debit: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_007: Failed to compensate float debit", e);
        }
    }
}