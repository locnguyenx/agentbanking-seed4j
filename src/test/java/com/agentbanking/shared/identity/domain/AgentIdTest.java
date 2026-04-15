package com.agentbanking.shared.identity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class AgentIdTest {

  @Nested
  @DisplayName("of")
  class OfTest {

    @Test
    void shouldCreateFromValue() {
      AgentId id = AgentId.of("AGT-001");

      assertThat(id.value()).isEqualTo("AGT-001");
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
      AgentId id1 = AgentId.of("AGT-001");
      AgentId id2 = AgentId.of("AGT-001");

      assertThat(id1).isEqualTo(id2);
      assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
  }
}
