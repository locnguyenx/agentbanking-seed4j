package com.agentbanking.settlement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_batch")
public class SettlementBatchEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "batch_date", nullable = false)
  private Instant batchDate;

  @Column(name = "type", length = 30, nullable = false)
  private String type;

  @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "total_fee", precision = 19, scale = 4)
  private BigDecimal totalFee;

  @Column(name = "total_commission", precision = 19, scale = 4)
  private BigDecimal totalCommission;

  @Column(name = "transaction_count", nullable = false)
  private Integer transactionCount;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "processed_at")
  private Instant processedAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Instant getBatchDate() { return batchDate; }
  public void setBatchDate(Instant batchDate) { this.batchDate = batchDate; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public BigDecimal getTotalFee() { return totalFee; }
  public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
  public BigDecimal getTotalCommission() { return totalCommission; }
  public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }
  public Integer getTransactionCount() { return transactionCount; }
  public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getProcessedAt() { return processedAt; }
  public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
