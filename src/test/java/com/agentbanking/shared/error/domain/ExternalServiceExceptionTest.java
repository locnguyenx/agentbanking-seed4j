package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("ExternalServiceException")
class ExternalServiceExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      ExternalServiceException exception = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_EXT_101);
      assertThat(exception.getMessage()).isEqualTo("CBS connector timeout");
      assertThat(exception.serviceName()).isEqualTo("CBS");
    }

    @Test
    void shouldCreateWithMessage() {
      ExternalServiceException exception = new ExternalServiceException(
        ErrorCode.ERR_EXT_101,
        "Custom CBS timeout message",
        "CBS"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_EXT_101);
      assertThat(exception.getMessage()).isEqualTo("Custom CBS timeout message");
      assertThat(exception.serviceName()).isEqualTo("CBS");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      ExternalServiceException exception = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      ExternalServiceException exception = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");

      assertThat(exception.httpStatus()).isEqualTo(504);
    }

    @Test
    void shouldReturnRetryActionCode() {
      ExternalServiceException exception = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");

      assertThat(exception.actionCode()).isEqualTo("RETRY");
    }
  }
}