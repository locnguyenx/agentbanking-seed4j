package com.agentbanking.billeradapter.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Component
public class BillerTimeoutWrapper {

    private static final Logger log = LoggerFactory.getLogger(BillerTimeoutWrapper.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final ExecutorService executor;

    public BillerTimeoutWrapper() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "biller-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    public <T> T executeWithTimeout(Supplier<T> action, int timeoutSeconds) {
        try {
            Future<T> future = executor.submit(action::get);
            return future.get(timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Biller request timed out after {} seconds", timeoutSeconds);
            throw new BillerTimeoutException("ERR_EXT_201: Biller request timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BillerTimeoutException("ERR_EXT_201: Biller request interrupted");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BillerTimeoutException) {
                throw (BillerTimeoutException) e.getCause();
            }
            throw new RuntimeException("ERR_EXT_202: Biller request failed", e.getCause());
        }
    }

    public <T> T executeWithDefaultTimeout(Supplier<T> action) {
        return executeWithTimeout(action, DEFAULT_TIMEOUT_SECONDS);
    }

    public static class BillerTimeoutException extends RuntimeException {
        public BillerTimeoutException(String message) {
            super(message);
        }
    }
}