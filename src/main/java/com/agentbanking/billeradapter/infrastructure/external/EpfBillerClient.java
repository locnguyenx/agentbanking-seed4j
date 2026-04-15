package com.agentbanking.billeradapter.infrastructure.external;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class EpfBillerClient {

    private static final Logger log = LoggerFactory.getLogger(EpfBillerClient.class);
    private static final int TIMEOUT_SECONDS = 30;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public BillDetails getBillDetails(String ref1, String ref2) {
        log.info("Calling EPF/KWSP bill inquiry API for ref: {}", ref1);
        
        try {
            Future<BillDetails> future = executor.submit(() -> simulateGetBillDetails(ref1));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("EPF bill inquiry timeout");
            throw new BillerTimeoutWrapper.BillerTimeoutException("ERR_EXT_201: EPF timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("ERR_EXT_201: EPF interrupted");
        } catch (ExecutionException e) {
            throw new RuntimeException("ERR_EXT_202: EPF error", e.getCause());
        }
    }

    private BillDetails simulateGetBillDetails(String ref1) {
        try {
            Thread.sleep(500 + (long) (Math.random() * 1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new BillDetails("EPF", ref1, "CUSTOMER NAME", new BigDecimal("500.00"), "EPF Contribution");
    }

    public PaymentResult payBill(String ref1, String ref2, BigDecimal amount) {
        log.info("Calling EPF/KWSP payment API for ref: {}, amount: {}", ref1, amount);
        
        try {
            Future<PaymentResult> future = executor.submit(() -> simulatePayBill(ref1, amount));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("EPF payment timeout");
            return new PaymentResult(false, null, null, "ERR_EXT_201");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, null, null, "ERR_EXT_201");
        } catch (ExecutionException e) {
            return new PaymentResult(false, null, null, "ERR_EXT_202");
        }
    }

    private PaymentResult simulatePayBill(String ref1, BigDecimal amount) {
        try {
            Thread.sleep(500 + (long) (Math.random() * 1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new PaymentResult(true,
            "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "KWSP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null);
    }

    public ReversalResult reverseBill(String transactionId, String reason) {
        log.info("Calling EPF reversal API for transaction: {}", transactionId);
        return new ReversalResult(false, null, "Reversal not supported for EPF");
    }
}