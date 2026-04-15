package com.agentbanking.shared.error.domain;

public class SystemException extends BusinessException {

  private final String message;

  public SystemException(ErrorCode errorCode) {
    super(errorCode);
    this.message = errorCode.message();
  }

  public SystemException(ErrorCode errorCode, String message) {
    super(errorCode);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}