package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.model.KycVerification;
import com.agentbanking.onboarding.domain.port.out.JpnPort;
import com.agentbanking.onboarding.domain.port.out.JpnVerificationResult;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JpnMockAdapter implements JpnPort {

    private static final Logger log = LoggerFactory.getLogger(JpnMockAdapter.class);

    @Override
    public JpnVerificationResult verifyMykad(String mykadNumber) {
        log.info("JPN: Verifying MyKad {}", maskMykad(mykadNumber));
        
        try {
            Thread.sleep(500 + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String extractedName = extractNameFromMykad(mykadNumber);
        String extractedAddress = "Malaysia";
        int age = calculateAge(mykadNumber);
        String amlStatus = "CLEAN";

        log.info("JPN: MyKad verified - name={}, age={}, aml={}", extractedName, age, amlStatus);

        return new JpnVerificationResult(true, extractedName, extractedAddress, age, amlStatus, null);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private String extractNameFromMykad(String mykadNumber) {
        return "User " + mykadNumber.substring(0, 6);
    }

    private int calculateAge(String mykadNumber) {
        int birthYear = Integer.parseInt(mykadNumber.substring(0, 2));
        int currentYear = java.time.Year.now().getValue() % 100;
        int fullYear = birthYear <= currentYear ? 2000 + birthYear : 1900 + birthYear;
        return java.time.Year.now().getValue() - fullYear;
    }

    private String maskMykad(String mykad) {
        if (mykad == null || mykad.length() < 6) return "****";
        return mykad.substring(0, 2) + "****" + mykad.substring(mykad.length() - 4);
    }
}