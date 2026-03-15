package com.coffiness.calfit.storage.db.core.applicant;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, Long> {

  Optional<ApplicantEntity> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  List<ApplicantEntity> findAllByTenantIdAndIdIn(String tenantId, List<Long> ids);

  List<ApplicantEntity> findByTenantIdAndIdIn(String tenantId, List<Long> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  List<ApplicantEntity> findAllByTenantIdAndIdInOrderByIdAsc(String tenantId, List<Long> ids);

  List<ApplicantEntity> findAllByIdIn(List<Long> ids);
}
