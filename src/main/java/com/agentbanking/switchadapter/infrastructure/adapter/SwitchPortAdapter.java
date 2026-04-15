package com.agentbanking.switchadapter.infrastructure.adapter;

import com.agentbanking.switchadapter.domain.model.IsoMessage;
import com.agentbanking.switchadapter.domain.port.out.SwitchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SwitchPortAdapter implements SwitchPort {

    private static final Logger log = LoggerFactory.getLogger(SwitchPortAdapter.class);

    @Override
    public IsoMessage send(IsoMessage message) {
        log.info("Sending ISO message to switch: MTI={}", message.mti());
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String responseMti = message.mti().replace("00", "10");
        java.util.Map<Integer, String> responseFields = new java.util.HashMap<>(message.fields());
        responseFields.put(39, "00");
        
        log.info("Received ISO response from switch: MTI={}, responseCode=00", responseMti);
        
        return new IsoMessage(responseMti, responseFields, null);
    }

    @Override
    public IsoMessage sendReversal(IsoMessage originalMessage) {
        log.info("Sending reversal to switch: original MTI={}", originalMessage.mti());
        
        String reversalMti = "0400";
        java.util.Map<Integer, String> responseFields = new java.util.HashMap<>();
        responseFields.put(39, "00");
        responseFields.put(11, originalMessage.fields().get(11));
        responseFields.put(37, originalMessage.fields().get(37));
        
        log.info("Reversal sent: MTI={}, responseCode=00", reversalMti);
        
        return new IsoMessage("0410", responseFields, null);
    }
}