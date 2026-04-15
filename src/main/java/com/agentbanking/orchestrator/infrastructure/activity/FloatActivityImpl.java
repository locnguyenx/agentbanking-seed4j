package com.agentbanking.orchestrator.infrastructure.activity;

import com.agentbanking.orchestrator.activity.FloatActivity;
import com.agentbanking.orchestrator.infrastructure.external.FloatClient;
import com.agentbanking.orchestrator.infrastructure.external.dto.LockResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class FloatActivityImpl implements FloatActivity {

    private static final Logger log = LoggerFactory.getLogger(FloatActivityImpl.class);

    private final FloatClient floatClient;

    public FloatActivityImpl(FloatClient floatClient) {
        this.floatClient = floatClient;
    }

    @Override
    public String lockAgentFloat(String agentId, String amount, String sagaId) {
        log.info("Locking agent float: agentId={}, amount={}, sagaId={}", agentId, amount, sagaId);
        try {
            Map<String, Object> request = Map.of(
                "amount", new BigDecimal(amount),
                "sagaId", sagaId
            );
            LockResultDTO result = floatClient.reserveFloat(agentId, request);
            if (!result.success()) {
                throw new RuntimeException(result.errorMessage());
            }
            return result.lockId();
        } catch (Exception e) {
            log.error("Failed to lock float: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_002: Failed to lock float", e);
        }
    }

    @Override
    public void debitAgentFloat(String agentId, String amount, String lockId) {
        log.info("Debiting agent float: agentId={}, amount={}, lockId={}", agentId, amount, lockId);
        try {
            Map<String, Object> request = Map.of(
                "amount", new BigDecimal(amount),
                "lockId", lockId
            );
            floatClient.debit(agentId, request);
        } catch (Exception e) {
            log.error("Failed to debit float: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_003: Failed to debit float", e);
        }
    }

    @Override
    public void creditAgentFloat(String agentId, String amount, String lockId) {
        log.info("Crediting agent float: agentId={}, amount={}, lockId={}", agentId, amount, lockId);
        try {
            Map<String, Object> request = Map.of(
                "amount", new BigDecimal(amount),
                "lockId", lockId
            );
            floatClient.credit(agentId, request);
        } catch (Exception e) {
            log.error("Failed to credit float: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_004: Failed to credit float", e);
        }
    }

    @Override
    public void releaseFloatLock(String lockId) {
        log.info("Releasing float lock: lockId={}", lockId);
        try {
            floatClient.releaseLock(lockId);
        } catch (Exception e) {
            log.error("Failed to release float lock: {}", e.getMessage());
            throw new RuntimeException("ERR_BIZ_005: Failed to release float lock", e);
        }
    }
}