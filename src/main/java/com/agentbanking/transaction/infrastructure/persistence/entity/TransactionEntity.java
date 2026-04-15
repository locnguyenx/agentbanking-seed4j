package com.agentbanking.transaction.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction")
public class TransactionEntity {

  @Id
  @Column(name = "id", length = 50)
  private String id;

  @Column(name = "type", length = 30, nullable = false)
  private String type;

  @Column(name = "amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  @Column(name = "agent_id", length = 50, nullable = false)
  private String agentId;

  @Column(name = "customer_account_id", length = 50)
  private String customerAccountId;

  @Column(name = "idempotency_key", length = 100, unique = true)
  private String idempotencyKey;

  @Column(name = "saga_execution_id", length = 100)
  private String sagaExecutionId;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "customer_fee", precision = 10, scale = 4)
  private BigDecimal customerFee;

  @Column(name = "agent_commission", precision = 10, scale = 4)
  private BigDecimal agentCommission;

  @Column(name = "bank_share", precision = 10, scale = 4)
  private BigDecimal bankShare;

  @Column(name = "customer_card_masked", length = 20)
  private String customerCardMasked;

  @Column(name = "error_code", length = 30)
  private String errorCode;

  @Column(name = "initiated_at", nullable = false)
  private Instant initiatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }
  public String getCustomerAccountId() { return customerAccountId; }
  public void setCustomerAccountId(String customerAccountId) { this.customerAccountId = customerAccountId; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
  public String getSagaExecutionId() { return sagaExecutionId; }
  public void setSagaExecutionId(String sagaExecutionId) { this.sagaExecutionId = sagaExecutionId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getCustomerFee() { return customerFee; }
  public void setCustomerFee(BigDecimal customerFee) { this.customerFee = customerFee; }
  public BigDecimal getAgentCommission() { return agentCommission; }
  public void setAgentCommission(BigDecimal agentCommission) { this.agentCommission = agentCommission; }
  public BigDecimal getBankShare() { return bankShare; }
  public void setBankShare(BigDecimal bankShare) { this.bankShare = bankShare; }
  public String getCustomerCardMasked() { return customerCardMasked; }
  public void setCustomerCardMasked(String customerCardMasked) { this.customerCardMasked = customerCardMasked; }
  public String getErrorCode() { return errorCode; }
  public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
  public Instant getInitiatedAt() { return initiatedAt; }
  public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
