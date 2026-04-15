package com.agentbanking.onboarding.domain.service;

public class DuplicateMykadException extends RuntimeException {
    private final String errorCode;

    public DuplicateMykadException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}