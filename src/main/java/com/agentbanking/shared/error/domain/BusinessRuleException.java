package com.agentbanking.shared.error.domain;

public class BusinessRuleException extends BusinessException {

  private final String message;

  public BusinessRuleException(ErrorCode errorCode) {
    super(errorCode);
    this.message = errorCode.message();
  }

  public BusinessRuleException(ErrorCode errorCode, String message) {
    super(errorCode);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}