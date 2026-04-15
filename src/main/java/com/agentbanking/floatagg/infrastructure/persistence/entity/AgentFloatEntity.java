package com.agentbanking.floatagg.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "agent_float")
public class AgentFloatEntity {

  @Id
  @Column(name = "agent_id", length = 50)
  private String agentId;

  @Column(name = "balance", precision = 19, scale = 4, nullable = false)
  private BigDecimal balance;

  @Column(name = "reserved_balance", precision = 19, scale = 4)
  private BigDecimal reservedBalance;

  @Column(name = "available_balance", precision = 19, scale = 4)
  private BigDecimal availableBalance;

  @Column(name = "currency", length = 3)
  private String currency;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Version
  @Column(name = "version")
  private Long version;

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }
  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }
  public BigDecimal getReservedBalance() { return reservedBalance; }
  public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
  public BigDecimal getAvailableBalance() { return availableBalance; }
  public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
