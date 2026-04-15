package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("SystemException")
class SystemExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      SystemException exception = new SystemException(ErrorCode.ERR_SYS_001);

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_SYS_001);
      assertThat(exception.getMessage()).isEqualTo("Internal server error");
    }

    @Test
    void shouldCreateWithMessage() {
      SystemException exception = new SystemException(
        ErrorCode.ERR_SYS_001,
        "Custom system error message"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_SYS_001);
      assertThat(exception.getMessage()).isEqualTo("Custom system error message");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      SystemException exception = new SystemException(ErrorCode.ERR_SYS_001);

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      SystemException exception = new SystemException(ErrorCode.ERR_SYS_001);

      assertThat(exception.httpStatus()).isEqualTo(500);
    }

    @Test
    void shouldReturnReviewActionCode() {
      SystemException exception = new SystemException(ErrorCode.ERR_SYS_001);

      assertThat(exception.actionCode()).isEqualTo("REVIEW");
    }
  }
}