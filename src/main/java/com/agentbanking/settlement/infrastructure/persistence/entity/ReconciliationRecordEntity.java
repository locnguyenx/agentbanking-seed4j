package com.agentbanking.settlement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_record")
public class ReconciliationRecordEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "transaction_id", length = 100, nullable = false)
  private String transactionId;

  @Column(name = "expected_amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal expectedAmount;

  @Column(name = "actual_amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal actualAmount;

  @Column(name = "discrepancy", precision = 19, scale = 4)
  private BigDecimal discrepancy;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "created_at")
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
  public BigDecimal getExpectedAmount() { return expectedAmount; }
  public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
  public BigDecimal getActualAmount() { return actualAmount; }
  public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
  public BigDecimal getDiscrepancy() { return discrepancy; }
  public void setDiscrepancy(BigDecimal discrepancy) { this.discrepancy = discrepancy; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getResolvedAt() { return resolvedAt; }
  public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
