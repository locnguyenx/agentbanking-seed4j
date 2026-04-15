package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("BusinessRuleException")
class BusinessRuleExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      BusinessRuleException exception = new BusinessRuleException(ErrorCode.ERR_BIZ_201);

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_BIZ_201);
      assertThat(exception.getMessage()).isEqualTo("Insufficient agent float balance");
    }

    @Test
    void shouldCreateWithMessage() {
      BusinessRuleException exception = new BusinessRuleException(
        ErrorCode.ERR_BIZ_201,
        "Insufficient float for transaction"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_BIZ_201);
      assertThat(exception.getMessage()).isEqualTo("Insufficient float for transaction");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      BusinessRuleException exception = new BusinessRuleException(ErrorCode.ERR_BIZ_201);

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      BusinessRuleException exception = new BusinessRuleException(ErrorCode.ERR_BIZ_201);

      assertThat(exception.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnDeclineActionCode() {
      BusinessRuleException exception = new BusinessRuleException(ErrorCode.ERR_BIZ_201);

      assertThat(exception.actionCode()).isEqualTo("DECLINE");
    }
  }
}