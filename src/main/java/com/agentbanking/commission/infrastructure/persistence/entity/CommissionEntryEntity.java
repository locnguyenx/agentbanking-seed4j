package com.agentbanking.commission.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commission_entry")
public class CommissionEntryEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "transaction_id", length = 100, nullable = false)
  private String transactionId;

  @Column(name = "agent_id", length = 50, nullable = false)
  private String agentId;

  @Column(name = "type", length = 30, nullable = false)
  private String type;

  @Column(name = "transaction_amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal transactionAmount;

  @Column(name = "commission_amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal commissionAmount;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "settled_at")
  private Instant settledAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getTransactionAmount() { return transactionAmount; }
  public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
  public BigDecimal getCommissionAmount() { return commissionAmount; }
  public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getSettledAt() { return settledAt; }
  public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
}
