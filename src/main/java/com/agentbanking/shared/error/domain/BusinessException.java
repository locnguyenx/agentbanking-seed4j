package com.agentbanking.shared.error.domain;

public abstract class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  protected BusinessException(ErrorCode errorCode) {
    super(errorCode.message());
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  protected BusinessException(String message) {
    super(message);
    this.errorCode = null; // For backward compatibility with existing code
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public int httpStatus() {
    return errorCode != null ? errorCode.httpStatus() : 500;
  }

  public String actionCode() {
    return errorCode != null ? errorCode.actionCode() : "REVIEW";
  }
}