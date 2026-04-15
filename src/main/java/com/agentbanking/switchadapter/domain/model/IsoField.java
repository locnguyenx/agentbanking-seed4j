package com.agentbanking.switchadapter.domain.model;

public record IsoField(int number, String name, String value, int format, int length) {
    public static final int FORMAT_ALPHA = 0;
    public static final int FORMAT_NUMERIC = 1;
    public static final int FORMAT_BINARY = 2;
    public static final int FORMAT_AMOUNT = 3;
}