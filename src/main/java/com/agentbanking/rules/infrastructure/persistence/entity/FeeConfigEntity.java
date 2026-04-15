package com.agentbanking.rules.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_config")
public class FeeConfigEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "transaction_type", nullable = false)
  private String transactionType;

  @Column(name = "agent_tier", nullable = false)
  private String agentTier;

  @Column(name = "fee_type", nullable = false)
  private String feeType;

  @Column(name = "customer_fee_value", precision = 10, scale = 4)
  private BigDecimal customerFeeValue;

  @Column(name = "agent_commission_value", precision = 10, scale = 4)
  private BigDecimal agentCommissionValue;

  @Column(name = "bank_share_value", precision = 10, scale = 4)
  private BigDecimal bankShareValue;

  @Column(name = "daily_limit_amount", precision = 19, scale = 4)
  private BigDecimal dailyLimitAmount;

  @Column(name = "daily_limit_count")
  private Integer dailyLimitCount;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getTransactionType() { return transactionType; }
  public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
  public String getAgentTier() { return agentTier; }
  public void setAgentTier(String agentTier) { this.agentTier = agentTier; }
  public String getFeeType() { return feeType; }
  public void setFeeType(String feeType) { this.feeType = feeType; }
  public BigDecimal getCustomerFeeValue() { return customerFeeValue; }
  public void setCustomerFeeValue(BigDecimal customerFeeValue) { this.customerFeeValue = customerFeeValue; }
  public BigDecimal getAgentCommissionValue() { return agentCommissionValue; }
  public void setAgentCommissionValue(BigDecimal agentCommissionValue) { this.agentCommissionValue = agentCommissionValue; }
  public BigDecimal getBankShareValue() { return bankShareValue; }
  public void setBankShareValue(BigDecimal bankShareValue) { this.bankShareValue = bankShareValue; }
  public BigDecimal getDailyLimitAmount() { return dailyLimitAmount; }
  public void setDailyLimitAmount(BigDecimal dailyLimitAmount) { this.dailyLimitAmount = dailyLimitAmount; }
  public Integer getDailyLimitCount() { return dailyLimitCount; }
  public void setDailyLimitCount(Integer dailyLimitCount) { this.dailyLimitCount = dailyLimitCount; }
  public LocalDate getEffectiveFrom() { return effectiveFrom; }
  public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
  public LocalDate getEffectiveTo() { return effectiveTo; }
  public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
