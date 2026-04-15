package com.agentbanking.onboarding.domain.model;

import java.time.Instant;
import java.util.UUID;

public enum KycStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    EXPIRED
}