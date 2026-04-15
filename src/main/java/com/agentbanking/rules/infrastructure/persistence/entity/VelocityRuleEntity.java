package com.agentbanking.rules.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "velocity_rule")
public class VelocityRuleEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "max_transactions_per_day")
  private Integer maxTransactionsPerDay;

  @Column(name = "max_amount_per_day", precision = 19, scale = 4)
  private BigDecimal maxAmountPerDay;

  @Column(name = "is_active")
  private Boolean isActive;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public Integer getMaxTransactionsPerDay() { return maxTransactionsPerDay; }
  public void setMaxTransactionsPerDay(Integer maxTransactionsPerDay) { this.maxTransactionsPerDay = maxTransactionsPerDay; }
  public BigDecimal getMaxAmountPerDay() { return maxAmountPerDay; }
  public void setMaxAmountPerDay(BigDecimal maxAmountPerDay) { this.maxAmountPerDay = maxAmountPerDay; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
