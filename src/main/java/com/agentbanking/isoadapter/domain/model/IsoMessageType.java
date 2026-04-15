package com.agentbanking.isoadapter.domain.model;

public enum IsoMessageType {
  AUTHORIZATION_REQUEST("0100"),
  AUTHORIZATION_RESPONSE("0110"),
  FINANCIAL_REQUEST("0200"),
  FINANCIAL_RESPONSE("0210"),
  REVERSAL_REQUEST("0400"),
  REVERSAL_RESPONSE("0410"),
  NETWORK_REQUEST("0800"),
  NETWORK_RESPONSE("0810");

  private final String code;

  IsoMessageType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static IsoMessageType fromCode(String code) {
    for (IsoMessageType type : values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown ISO message code: " + code);
  }
}
