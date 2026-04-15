package com.agentbanking.shared.identity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class TransactionIdTest {

  @Nested
  @DisplayName("generate")
  class GenerateTest {

    @Test
    void shouldGenerateUniqueId() {
      TransactionId id1 = TransactionId.generate();
      TransactionId id2 = TransactionId.generate();

      assertThat(id1.value()).isNotBlank();
      assertThat(id2.value()).isNotBlank();
      assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldGenerateWithPrefix() {
      TransactionId id = TransactionId.generate();

      assertThat(id.value()).startsWith("TXN-");
    }
  }

  @Nested
  @DisplayName("of")
  class OfTest {

    @Test
    void shouldCreateFromValue() {
      TransactionId id = TransactionId.of("TXN-001");

      assertThat(id.value()).isEqualTo("TXN-001");
    }
  }
}
