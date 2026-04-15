package com.agentbanking.shared.identity.domain;

public record AgentId(String value) {

  public static AgentId of(String value) {
    return new AgentId(value);
  }
}
