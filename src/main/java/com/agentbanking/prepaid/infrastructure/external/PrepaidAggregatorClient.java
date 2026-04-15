package com.agentbanking.prepaid.infrastructure.external;

import com.agentbanking.prepaid.domain.port.out.BalanceCheckResult;
import com.agentbanking.prepaid.domain.port.out.TopUpGatewayPort;
import com.agentbanking.prepaid.domain.port.out.TopUpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class PrepaidAggregatorClient implements TopUpGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(PrepaidAggregatorClient.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public TopUpResult topUp(String providerCode, String phoneNumber, BigDecimal amount) {
        log.info("Calling prepaid aggregator for {} top-up of {} to {}", providerCode, amount, phoneNumber);
        
        try {
            Future<TopUpResult> future = executor.submit(() -> simulateTopUp(providerCode, phoneNumber, amount));
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Prepaid aggregator timeout for {}", providerCode);
            return new TopUpResult(false, null, null, "ERR_EXT_301");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TopUpResult(false, null, null, "ERR_EXT_301");
        } catch (ExecutionException e) {
            log.error("Prepaid aggregator error for {}: {}", providerCode, e.getMessage());
            return new TopUpResult(false, null, null, "ERR_EXT_302");
        }
    }

    private TopUpResult simulateTopUp(String providerCode, String phoneNumber, BigDecimal amount) {
        try {
            Thread.sleep(500 + (long) (Math.random() * 2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String txnId = "TXN-" + providerCode.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String confId = providerCode.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        return new TopUpResult(true, txnId, confId, null);
    }

    @Override
    public BalanceCheckResult checkBalance(String providerCode, String phoneNumber) {
        log.info("Checking balance for {} on {}", providerCode, phoneNumber);
        
        try {
            Future<BalanceCheckResult> future = executor.submit(() -> simulateBalanceCheck(providerCode, phoneNumber));
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Balance check timeout for {}", providerCode);
            return new BalanceCheckResult(false, null, null, "ERR_EXT_301");
        } catch (Exception e) {
            return new BalanceCheckResult(false, null, null, "ERR_EXT_302");
        }
    }

    private BalanceCheckResult simulateBalanceCheck(String providerCode, String phoneNumber) {
        try {
            Thread.sleep(300 + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        BigDecimal balance = new BigDecimal("25.00");
        return new BalanceCheckResult(true, balance, "Prepaid balance", null);
    }
}