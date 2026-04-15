package com.agentbanking.floatagg.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "float_transaction")
public class FloatTransactionEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "agent_id", length = 50, nullable = false)
  private String agentId;

  @Column(name = "transaction_id", length = 100)
  private String transactionId;

  @Column(name = "type", length = 30, nullable = false)
  private String type;

  @Column(name = "amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  @Column(name = "balance_before", precision = 19, scale = 4)
  private BigDecimal balanceBefore;

  @Column(name = "balance_after", precision = 19, scale = 4)
  private BigDecimal balanceAfter;

  @Column(name = "created_at")
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }
  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public BigDecimal getBalanceBefore() { return balanceBefore; }
  public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
  public BigDecimal getBalanceAfter() { return balanceAfter; }
  public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
