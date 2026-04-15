package com.agentbanking.transaction.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecordEntity {

  @Id
  @Column(name = "idempotency_key", length = 100)
  private String idempotencyKey;

  @Column(name = "response_payload", columnDefinition = "jsonb", nullable = false)
  private String responsePayload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
  public String getResponsePayload() { return responsePayload; }
  public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
