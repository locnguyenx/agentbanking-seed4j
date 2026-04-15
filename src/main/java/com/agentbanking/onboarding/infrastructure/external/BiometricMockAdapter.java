package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.port.out.BiometricMatchResult;
import com.agentbanking.onboarding.domain.port.out.BiometricPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BiometricMockAdapter implements BiometricPort {

    private static final Logger log = LoggerFactory.getLogger(BiometricMockAdapter.class);

    @Override
    public BiometricMatchResult verifyThumbprint(String biometricTemplate) {
        log.info("Biometric: Verifying thumbprint");
        
        try {
            Thread.sleep(300 + (long) (Math.random() * 700));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean match = Math.random() > 0.1;
        int confidence = match ? 95 + (int)(Math.random() * 5) : 50 + (int)(Math.random() * 20);
        
        log.info("Biometric: Thumbprint verified - match={}, confidence={}", match, confidence);

        return new BiometricMatchResult(match, confidence, null);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}