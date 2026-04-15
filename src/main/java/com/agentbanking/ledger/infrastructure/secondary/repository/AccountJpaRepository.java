package com.agentbanking.ledger.infrastructure.secondary.repository;

import com.agentbanking.ledger.infrastructure.secondary.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

  Optional<AccountEntity> findByAccountCode(String accountCode);
}
