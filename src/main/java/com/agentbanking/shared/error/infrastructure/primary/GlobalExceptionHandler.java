package com.agentbanking.shared.error.infrastructure.primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.agentbanking.shared.error.domain.*;

@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<GlobalErrorSchema> handleValidationException(
    ValidationException ex,
    WebRequest request
  ) {
    log.warn("Validation error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(schema);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<GlobalErrorSchema> handleBusinessRuleException(
    BusinessRuleException ex,
    WebRequest request
  ) {
    log.warn("Business rule violation: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(schema);
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<GlobalErrorSchema> handleExternalServiceException(
    ExternalServiceException ex,
    WebRequest request
  ) {
    log.error("External service error: {} - {} from {}", ex.getErrorCode(), ex.getMessage(), ex.serviceName());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    HttpStatus status = ex.httpStatus() == 504 ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
    return ResponseEntity.status(status).body(schema);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<GlobalErrorSchema> handleAuthenticationException(
    AuthenticationException ex,
    WebRequest request
  ) {
    log.warn("Authentication error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    HttpStatus status = ex.httpStatus() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
    return ResponseEntity.status(status).body(schema);
  }

  @ExceptionHandler(SystemException.class)
  public ResponseEntity<GlobalErrorSchema> handleSystemException(
    SystemException ex,
    WebRequest request
  ) {
    log.error("System error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(schema);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GlobalErrorSchema> handleGenericException(
    Exception ex,
    WebRequest request
  ) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ErrorCode.ERR_SYS_001.name(),
      "An internal error occurred",
      "REVIEW"
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(schema);
  }
}