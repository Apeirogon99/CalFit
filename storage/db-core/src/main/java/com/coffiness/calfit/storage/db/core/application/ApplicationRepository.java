package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
  boolean existsByApplicantIdAndRecruitmentId(Long applicantId, Long recruitmentId);

  Optional<ApplicationEntity> findByIdAndStatus(Long id, EntityStatus status);

  @Query(
      value =
          "SELECT a.tenant_id FROM applications a"
              + " WHERE a.id = :applicationId"
              + " AND a.status = 'ACTIVE'",
      nativeQuery = true)
  Optional<String> findTenantIdById(@Param("applicationId") Long applicationId);

  List<ApplicationEntity> findByRecruitmentIdAndStatus(Long recruitmentId, EntityStatus status);

  // 현재 tenant 기준으로 채용 공고의 활성 지원자 수를 카운팅
  long countByTenantIdAndRecruitmentIdAndStatus(
      String tenantId, Long recruitmentId, EntityStatus status);

  List<ApplicationEntity> findByRecruitmentIdAndRecruitmentProcessIdAndStatus(
      Long recruitmentId, Long recruitmentProcessId, EntityStatus status);

  List<ApplicationEntity> findByStatus(EntityStatus status);

  long countByStatus(EntityStatus status);

  @Query(
      "SELECT a.recruitmentProcessId, COUNT(a) FROM ApplicationEntity a"
          + " WHERE a.status = :status"
          + " GROUP BY a.recruitmentProcessId")
  List<Object[]> countGroupByRecruitmentProcessId(@Param("status") EntityStatus status);

  // 현재 tenant의 채용 공고/단계별 활성 지원자 수를 집계
  @Query(
      "SELECT a.recruitmentId, a.recruitmentProcessId, COUNT(a) FROM ApplicationEntity a"
          + " WHERE a.tenantId = :tenantId"
          + " AND a.recruitmentId IN :recruitmentIds"
          + " AND a.status = :status"
          + " GROUP BY a.recruitmentId, a.recruitmentProcessId")
  List<Object[]> countGroupByRecruitmentIdAndRecruitmentProcessId(
      @Param("tenantId") String tenantId,
      @Param("recruitmentIds") List<Long> recruitmentIds,
      @Param("status") EntityStatus status);

  @Query(
      "SELECT a.recruitmentId, COUNT(a) FROM ApplicationEntity a"
          + " WHERE a.status = :status"
          + " GROUP BY a.recruitmentId")
  List<Object[]> countGroupByRecruitmentId(@Param("status") EntityStatus status);

  @Query(
      "SELECT year(a.createdAt), month(a.createdAt), COUNT(a) FROM ApplicationEntity a"
          + " WHERE a.status = :status AND a.createdAt >= :from"
          + " GROUP BY year(a.createdAt), month(a.createdAt)"
          + " ORDER BY year(a.createdAt), month(a.createdAt)")
  List<Object[]> countGroupByMonth(
      @Param("status") EntityStatus status, @Param("from") LocalDateTime from);
}
