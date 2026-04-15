package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("ErrorCode")
class ErrorCodeTest {

  @Nested
  class CategoryResolution {

    @Test
    void shouldResolveValidationCategory() {
      ErrorCode code = ErrorCode.ERR_VAL_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.VALIDATION);
    }

    @Test
    void shouldResolveBusinessCategory() {
      ErrorCode code = ErrorCode.ERR_BIZ_201;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.BUSINESS);
    }

    @Test
    void shouldResolveExternalCategory() {
      ErrorCode code = ErrorCode.ERR_EXT_101;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.EXTERNAL);
    }

    @Test
    void shouldResolveAuthenticationCategory() {
      ErrorCode code = ErrorCode.ERR_AUTH_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.AUTHENTICATION);
    }

    @Test
    void shouldResolveSystemCategory() {
      ErrorCode code = ErrorCode.ERR_SYS_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.SYSTEM);
    }

    @Test
    void shouldResolveIsoCategory() {
      ErrorCode code = ErrorCode.ERR_ISO_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.ISO_TRANSLATION);
    }

    @Test
    void shouldResolveHsmCategory() {
      ErrorCode code = ErrorCode.ERR_HSM_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.HSM_WRAPPER);
    }

    @Test
    void shouldResolveCbsCategory() {
      ErrorCode code = ErrorCode.ERR_CBS_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.CBS_CONNECTOR);
    }
  }

  @Nested
  class ActionCodeResolution {

    @Test
    void shouldReturnDeclineForValidationErrors() {
      assertThat(ErrorCode.ERR_VAL_001.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnDeclineForBusinessErrors() {
      assertThat(ErrorCode.ERR_BIZ_201.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnRetryForExternalErrors() {
      assertThat(ErrorCode.ERR_EXT_101.actionCode()).isEqualTo("RETRY");
    }

    @Test
    void shouldReturnDeclineForAuthErrors() {
      assertThat(ErrorCode.ERR_AUTH_001.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnReviewForSystemErrors() {
      assertThat(ErrorCode.ERR_SYS_001.actionCode()).isEqualTo("REVIEW");
    }
  }

  @Nested
  class FromCodeLookup {

    @Test
    void shouldFindCodeByValue() {
      assertThat(ErrorCode.fromCode("ERR_VAL_001")).isPresent().contains(ErrorCode.ERR_VAL_001);
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
      assertThat(ErrorCode.fromCode("UNKNOWN")).isEmpty();
    }
  }

  @Nested
  class HttpStatusMapping {

    @Test
    void shouldReturnBadRequestForValidation() {
      assertThat(ErrorCode.ERR_VAL_001.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnBadRequestForBusiness() {
      assertThat(ErrorCode.ERR_BIZ_201.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnGatewayTimeoutForExternalTimeout() {
      assertThat(ErrorCode.ERR_EXT_101.httpStatus()).isEqualTo(504);
    }

    @Test
    void shouldReturnBadGatewayForExternalError() {
      assertThat(ErrorCode.ERR_EXT_102.httpStatus()).isEqualTo(502);
    }

    @Test
    void shouldReturnUnauthorizedForAuth() {
      assertThat(ErrorCode.ERR_AUTH_001.httpStatus()).isEqualTo(401);
    }

    @Test
    void shouldReturnForbiddenForAccountLocked() {
      assertThat(ErrorCode.ERR_AUTH_004.httpStatus()).isEqualTo(429);
    }

    @Test
    void shouldReturnInternalServerErrorForSystem() {
      assertThat(ErrorCode.ERR_SYS_001.httpStatus()).isEqualTo(500);
    }
  }
}