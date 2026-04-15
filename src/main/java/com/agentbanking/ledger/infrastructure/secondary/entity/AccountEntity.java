package com.agentbanking.ledger.infrastructure.secondary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class AccountEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "account_code", length = 50, nullable = false, unique = true)
  private String accountCode;

  @Column(name = "account_type", length = 30, nullable = false)
  private String accountType;

  @Column(name = "balance", precision = 19, scale = 4)
  private BigDecimal balance;

  @Column(name = "currency", length = 3)
  private String currency;

  @Column(name = "created_at")
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getAccountCode() { return accountCode; }
  public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
