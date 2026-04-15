package com.agentbanking.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "agent")
public class AgentEntity {

  @Id
  @Column(name = "id", length = 50)
  private String id;

  @Column(name = "agent_code", length = 50, nullable = false)
  private String agentCode;

  @Column(name = "business_registration_number", length = 100, nullable = false, unique = true)
  private String businessRegistrationNumber;

  @Column(name = "name", length = 200, nullable = false)
  private String name;

  @Column(name = "type", length = 30, nullable = false)
  private String type;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "max_float_limit", precision = 19, scale = 4)
  private BigDecimal maxFloatLimit;

  @Column(name = "owner_name", length = 200)
  private String ownerName;

  @Column(name = "mykad_number", length = 12)
  private String mykadNumber;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "email", length = 200)
  private String email;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "registered_at")
  private Instant registeredAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getAgentCode() { return agentCode; }
  public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
  public String getBusinessRegistrationNumber() { return businessRegistrationNumber; }
  public void setBusinessRegistrationNumber(String businessRegistrationNumber) { this.businessRegistrationNumber = businessRegistrationNumber; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getMaxFloatLimit() { return maxFloatLimit; }
  public void setMaxFloatLimit(BigDecimal maxFloatLimit) { this.maxFloatLimit = maxFloatLimit; }
  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
  public String getMykadNumber() { return mykadNumber; }
  public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public Instant getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
}
