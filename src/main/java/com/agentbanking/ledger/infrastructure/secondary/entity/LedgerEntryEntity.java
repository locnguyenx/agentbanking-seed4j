package com.agentbanking.ledger.infrastructure.secondary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntryEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "transaction_id", length = 100, nullable = false)
  private String transactionId;

  @Column(name = "account_code", length = 50, nullable = false)
  private String accountCode;

  @Column(name = "entry_type", length = 10, nullable = false)
  private String entryType;

  @Column(name = "account_type", length = 30, nullable = false)
  private String accountType;

  @Column(name = "amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "created_at")
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
  public String getAccountCode() { return accountCode; }
  public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
  public String getEntryType() { return entryType; }
  public void setEntryType(String entryType) { this.entryType = entryType; }
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
