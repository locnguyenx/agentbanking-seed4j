package com.agentbanking.switchadapter.domain.model;

public record IsoMessage(String mti, java.util.Map<Integer, String> fields, byte[] bitmap) {}