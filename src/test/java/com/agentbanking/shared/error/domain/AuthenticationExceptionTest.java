package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("AuthenticationException")
class AuthenticationExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      AuthenticationException exception = new AuthenticationException(ErrorCode.ERR_AUTH_001);

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_AUTH_001);
      assertThat(exception.getMessage()).isEqualTo("Invalid OAuth2 token");
    }

    @Test
    void shouldCreateWithMessage() {
      AuthenticationException exception = new AuthenticationException(
        ErrorCode.ERR_AUTH_001,
        "Custom auth error message"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_AUTH_001);
      assertThat(exception.getMessage()).isEqualTo("Custom auth error message");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      AuthenticationException exception = new AuthenticationException(ErrorCode.ERR_AUTH_001);

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      AuthenticationException exception = new AuthenticationException(ErrorCode.ERR_AUTH_001);

      assertThat(exception.httpStatus()).isEqualTo(401);
    }

    @Test
    void shouldReturnDeclineActionCode() {
      AuthenticationException exception = new AuthenticationException(ErrorCode.ERR_AUTH_001);

      assertThat(exception.actionCode()).isEqualTo("DECLINE");
    }
  }
}