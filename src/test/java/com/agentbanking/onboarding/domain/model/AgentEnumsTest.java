package com.agentbanking.onboarding.domain.model;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.onboarding.domain.AgentStatus;
import com.agentbanking.shared.onboarding.domain.AgentType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@UnitTest
class AgentEnumsTest {

    @Test
    void agentType_hasMicroStandardPremier() {
        assertNotNull(AgentType.MICRO);
        assertNotNull(AgentType.STANDARD);
        assertNotNull(AgentType.PREMIER);
        assertEquals(3, AgentType.values().length);
    }

    @Test
    void agentStatus_hasExpectedValues() {
        assertNotNull(AgentStatus.PENDING_APPROVAL);
        assertNotNull(AgentStatus.ACTIVE);
        assertNotNull(AgentStatus.DEACTIVATED);
    }

    @Test
    void agentType_valuesAreUnique() {
        assertEquals("MICRO", AgentType.MICRO.name());
        assertEquals("STANDARD", AgentType.STANDARD.name());
        assertEquals("PREMIER", AgentType.PREMIER.name());
    }

    @Test
    void agentStatus_valuesAreUnique() {
        assertEquals("PENDING_APPROVAL", AgentStatus.PENDING_APPROVAL.name());
        assertEquals("ACTIVE", AgentStatus.ACTIVE.name());
        assertEquals("DEACTIVATED", AgentStatus.DEACTIVATED.name());
    }
}