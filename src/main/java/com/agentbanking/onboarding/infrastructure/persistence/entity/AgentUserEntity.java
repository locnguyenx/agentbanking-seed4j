package com.agentbanking.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_user")
public class AgentUserEntity {

  @Id
  @Column(name = "id", length = 50)
  private String id;

  @Column(name = "agent_id", length = 50, nullable = false)
  private String agentId;

  @Column(name = "username", length = 100, nullable = false, unique = true)
  private String username;

  @Column(name = "role", length = 30, nullable = false)
  private String role;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
