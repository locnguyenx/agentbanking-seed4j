package com.agentbanking.commission.infrastructure.messaging;

import com.agentbanking.commission.domain.model.CommissionEvent;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.shared.money.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CommissionEventProducer {

  private static final Logger log = LoggerFactory.getLogger(CommissionEventProducer.class);
  private static final String BINDING = "commissionEvents-out-0";

  private final StreamBridge streamBridge;

  public CommissionEventProducer(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  public void emitCommissionEvent(String transactionId, String agentId,
                                    CommissionType type, Money transactionAmount,
                                    Money commissionAmount) {
    CommissionEvent event = new CommissionEvent(
      UUID.randomUUID().toString(),
      transactionId,
      agentId,
      type,
      transactionAmount,
      commissionAmount,
      Instant.now()
    );

    boolean sent = streamBridge.send(BINDING, event);
    if (sent) {
      log.info("Commission event emitted: {} for agent {}", type, agentId);
    } else {
      log.error("Failed to emit commission event: {} for agent {}", type, agentId);
    }
  }
}
