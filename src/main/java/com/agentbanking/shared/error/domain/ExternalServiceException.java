package com.agentbanking.shared.error.domain;

public class ExternalServiceException extends BusinessException {

  private final String serviceName;
  private final String message;

  public ExternalServiceException(ErrorCode errorCode, String serviceName) {
    super(errorCode);
    this.serviceName = serviceName;
    this.message = errorCode.message();
  }

  public ExternalServiceException(ErrorCode errorCode, String message, String serviceName) {
    super(errorCode);
    this.serviceName = serviceName;
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public String serviceName() {
    return serviceName;
  }
}