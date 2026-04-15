package com.agentbanking.shared.error.domain;

public class AuthenticationException extends BusinessException {

  private final String message;

  public AuthenticationException(ErrorCode errorCode) {
    super(errorCode);
    this.message = errorCode.message();
  }

  public AuthenticationException(ErrorCode errorCode, String message) {
    super(errorCode);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}