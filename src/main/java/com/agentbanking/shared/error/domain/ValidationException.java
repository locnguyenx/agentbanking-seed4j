package com.agentbanking.shared.error.domain;

public class ValidationException extends BusinessException {

  private final String field;
  private final String message;

  public ValidationException(ErrorCode errorCode, String field) {
    super(errorCode);
    this.field = field;
    this.message = errorCode.message() + " - Field: " + field;
  }

  public ValidationException(ErrorCode errorCode, String message, String field) {
    super(errorCode);
    this.field = field;
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public String field() {
    return field;
  }
}