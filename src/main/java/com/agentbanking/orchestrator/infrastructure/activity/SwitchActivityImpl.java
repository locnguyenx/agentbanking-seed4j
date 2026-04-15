package com.agentbanking.orchestrator.infrastructure.activity;

import com.agentbanking.orchestrator.activity.SwitchActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SwitchActivityImpl implements SwitchActivity {

    private static final Logger log = LoggerFactory.getLogger(SwitchActivityImpl.class);

    @Override
    public String sendWithdrawal(String agentId, String customerAccountId, String amount, String stan) {
        log.info("Sending withdrawal to switch: agentId={}, account={}, amount={}, stan={}", 
            agentId, customerAccountId, amount, stan);
        
        String stanGenerated = stan != null ? stan : generateStan();
        String rrn = generateRrn();
        
        log.info("Withdrawal processed: stan={}, rrn={}", stanGenerated, rrn);
        
        return stanGenerated;
    }

    @Override
    public String sendDeposit(String agentId, String customerAccountId, String amount, String stan) {
        log.info("Sending deposit to switch: agentId={}, account={}, amount={}, stan={}", 
            agentId, customerAccountId, amount, stan);
        
        String stanGenerated = stan != null ? stan : generateStan();
        String rrn = generateRrn();
        
        log.info("Deposit processed: stan={}, rrn={}", stanGenerated, rrn);
        
        return stanGenerated;
    }

    @Override
    public void sendReversal(String originalStan, String reason) {
        log.info("Sending reversal to switch: originalStan={}, reason={}", originalStan, reason);
        
        String reversalStan = generateStan();
        String reversalRrn = generateRrn();
        
        log.info("Reversal sent: MTI=0400, stan={}, rrn={}", reversalStan, reversalRrn);
    }

    private String generateStan() {
        return String.format("%06d", UUID.randomUUID().toString().hashCode() % 1000000);
    }

    private String generateRrn() {
        return String.format("%12d", System.currentTimeMillis() % 1000000000000L);
    }
}