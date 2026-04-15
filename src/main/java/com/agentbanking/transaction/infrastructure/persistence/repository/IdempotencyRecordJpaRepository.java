package com.agentbanking.transaction.infrastructure.persistence.repository;

import com.agentbanking.transaction.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, String> {

  Optional<IdempotencyRecordEntity> findByIdempotencyKey(String idempotencyKey);
}
