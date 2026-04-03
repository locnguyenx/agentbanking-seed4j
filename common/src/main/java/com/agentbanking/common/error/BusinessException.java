package com.agentbanking.common.error;

public class BusinessException extends RuntimeException {
    private final String code;
    private final String actionCode;

    public BusinessException(String code, String message, String actionCode) {
        super(message);
        this.code = code;
        this.actionCode = actionCode;
    }

    public String getCode() { return code; }
    public String getActionCode() { return actionCode; }
}
