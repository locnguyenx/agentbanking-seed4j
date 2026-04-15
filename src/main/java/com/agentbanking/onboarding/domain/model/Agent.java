package com.agentbanking.onboarding.domain.model;

import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.shared.onboarding.domain.AgentType;
import java.time.Instant;
import java.util.UUID;

public record Agent(
    UUID id,
    String agentCode,
    AgentType agentType,
    AgentStatus status,
    String businessName,
    String businessRegistrationNumber,
    String ownerName,
    String mykadNumber,
    String phoneNumber,
    String email,
    String address,
    Instant registeredAt,
    Instant approvedAt
) {
    public static Agent register(
        String businessName,
        String businessRegistrationNumber,
        String ownerName,
        String mykadNumber,
        String phoneNumber,
        String email,
        String address,
        AgentType agentType
    ) {
        return new Agent(
            UUID.randomUUID(),
            "AGT-" + System.currentTimeMillis(),
            agentType,
            AgentStatus.PENDING_APPROVAL,
            businessName,
            businessRegistrationNumber,
            ownerName,
            mykadNumber,
            phoneNumber,
            email,
            address,
            Instant.now(),
            null
        );
    }

    public Agent approve() {
        return new Agent(
            id, agentCode, agentType, AgentStatus.ACTIVE,
            businessName, businessRegistrationNumber, ownerName,
            mykadNumber, phoneNumber, email, address,
            registeredAt, Instant.now()
        );
    }

    public Agent reject() {
        return new Agent(
            id, agentCode, agentType, AgentStatus.DEACTIVATED,
            businessName, businessRegistrationNumber, ownerName,
            mykadNumber, phoneNumber, email, address,
            registeredAt, null
        );
    }

    public Agent completeKyc() {
        return new Agent(
            id, agentCode, agentType, AgentStatus.ACTIVE,
            businessName, businessRegistrationNumber, ownerName,
            mykadNumber, phoneNumber, email, address,
            registeredAt, Instant.now()
        );
    }
}