package com.agentbanking.isoadapter.domain.model;

import java.util.HashMap;
import java.util.Map;

public record IsoMessage(String mti, Map<Integer, String> fields, byte[] bitmap) {

  public static IsoMessageBuilder builder(String mti) {
    return new IsoMessageBuilder(mti);
  }

  public String getField(int fieldNumber) {
    return fields.get(fieldNumber);
  }

  public static class IsoMessageBuilder {
    private final String mti;
    private final Map<Integer, String> fields = new HashMap<>();

    public IsoMessageBuilder(String mti) {
      this.mti = mti;
    }

    public IsoMessageBuilder field(int number, String value) {
      fields.put(number, value);
      return this;
    }

    public IsoMessage build() {
      return new IsoMessage(mti, Map.copyOf(fields), new byte[16]);
    }
  }
}
