package com.coffiness.calfit.storage.db.core.payment;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface BillingKeyRepository extends JpaRepository<BillingKeyEntity, Long> {

  Optional<BillingKeyEntity> findByWorkspaceIdAndActiveAndStatus(
      String workspaceId, boolean active, EntityStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<BillingKeyEntity> findByWorkspaceIdAndActiveIsTrueAndStatus(
      String workspaceId, EntityStatus status);
}
