package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("GlobalErrorSchema")
class GlobalErrorSchemaTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithAllFields() {
      Instant now = Instant.now();
      GlobalErrorSchema schema = new GlobalErrorSchema(
        "ERR_VAL_001",
        "Invalid request",
        "DECLINE",
        "trace-123",
        now
      );

      assertThat(schema.code()).isEqualTo("ERR_VAL_001");
      assertThat(schema.message()).isEqualTo("Invalid request");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
      assertThat(schema.traceId()).isEqualTo("trace-123");
      assertThat(schema.timestamp()).isEqualTo(now);
    }

    @Test
    void shouldCreateWithFactoryMethod() {
      GlobalErrorSchema schema = GlobalErrorSchema.of(
        "ERR_BIZ_201",
        "Insufficient float",
        "DECLINE"
      );

      assertThat(schema.code()).isEqualTo("ERR_BIZ_201");
      assertThat(schema.message()).isEqualTo("Insufficient float");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
      assertThat(schema.traceId()).isNotNull();
      assertThat(schema.timestamp()).isNotNull();
    }
  }

  @Nested
  class ActionCodeValidation {

    @Test
    void shouldAcceptValidActionCode_DECLINE() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_VAL_001", "test", "DECLINE");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldAcceptValidActionCode_RETRY() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_EXT_101", "test", "RETRY");
      assertThat(schema.actionCode()).isEqualTo("RETRY");
    }

    @Test
    void shouldAcceptValidActionCode_REVIEW() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_SYS_001", "test", "REVIEW");
      assertThat(schema.actionCode()).isEqualTo("REVIEW");
    }
  }
}