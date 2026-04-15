package com.agentbanking.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.error.domain.*;

@UnitTest
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Nested
  class ValidationExceptionHandling {

    @Test
    void shouldHandleValidationException() {
      ValidationException ex = new ValidationException(ErrorCode.ERR_VAL_001, "amount");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleValidationException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_VAL_001");
      assertThat(response.getBody().actionCode()).isEqualTo("DECLINE");
    }
  }

  @Nested
  class BusinessRuleExceptionHandling {

    @Test
    void shouldHandleBusinessRuleException() {
      BusinessRuleException ex = new BusinessRuleException(ErrorCode.ERR_BIZ_201);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleBusinessRuleException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_BIZ_201");
    }
  }

  @Nested
  class ExternalServiceExceptionHandling {

    @Test
    void shouldHandleExternalServiceException() {
      ExternalServiceException ex = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleExternalServiceException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(504);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_EXT_101");
      assertThat(response.getBody().actionCode()).isEqualTo("RETRY");
    }
  }

  @Nested
  class AuthenticationExceptionHandling {

    @Test
    void shouldHandleAuthenticationException() {
      AuthenticationException ex = new AuthenticationException(ErrorCode.ERR_AUTH_001);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleAuthenticationException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(401);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_AUTH_001");
    }
  }

  @Nested
  class SystemExceptionHandling {

    @Test
    void shouldHandleSystemException() {
      SystemException ex = new SystemException(ErrorCode.ERR_SYS_001);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleSystemException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(500);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_SYS_001");
      assertThat(response.getBody().actionCode()).isEqualTo("REVIEW");
    }
  }

  @Nested
  class GenericExceptionHandling {

    @Test
    void shouldHandleGenericException() {
      RuntimeException ex = new RuntimeException("Unexpected error");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleGenericException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(500);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_SYS_001");
    }
  }
}