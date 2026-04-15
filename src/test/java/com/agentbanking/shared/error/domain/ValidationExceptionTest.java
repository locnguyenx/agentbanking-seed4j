package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("ValidationException")
class ValidationExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "amount");

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_VAL_001);
      assertThat(exception.getMessage()).contains("amount");
    }

    @Test
    void shouldCreateWithMessage() {
      ValidationException exception = new ValidationException(
        ErrorCode.ERR_VAL_001,
        "Invalid request",
        "amount"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_VAL_001);
      assertThat(exception.getMessage()).isEqualTo("Invalid request");
      assertThat(exception.field()).isEqualTo("amount");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnDeclineActionCode() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception.actionCode()).isEqualTo("DECLINE");
    }
  }
}