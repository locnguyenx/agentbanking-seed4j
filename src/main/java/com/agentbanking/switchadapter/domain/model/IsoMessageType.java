package com.agentbanking.switchadapter.domain.model;

public enum IsoMessageType {
    MTI_0100_AUTHORIZATION_REQUEST("0100"),
    MTI_0110_AUTHORIZATION_RESPONSE("0110"),
    MTI_0120_REVERSAL_REQUEST("0120"),
    MTI_0121_REVERSAL_RESPONSE("0121"),
    MTI_0200_NETWORK_MANAGEMENT("0200"),
    MTI_0210_NETWORK_MANAGEMENT_RESPONSE("0210"),
    MTI_0400_REVERSAL("0400"),
    MTI_0410_REVERSAL_RESPONSE("0410");

    private final String value;

    IsoMessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isRequest() {
        return value.endsWith("00");
    }

    public String getResponseMti() {
        return String.valueOf(Integer.parseInt(value) + 10);
    }
}