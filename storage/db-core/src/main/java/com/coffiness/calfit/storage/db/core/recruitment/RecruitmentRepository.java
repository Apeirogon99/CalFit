package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentRepository extends JpaRepository<RecruitmentEntity, Long> {
  Page<RecruitmentEntity> findByRecruitmentStatus(
      RecruitmentStatus recruitmentStatus, Pageable pageable);

  Page<RecruitmentEntity> findByStatus(EntityStatus status, Pageable pageable);

  Page<RecruitmentEntity> findByRecruitmentStatusAndStatus(
      RecruitmentStatus recruitmentStatus, EntityStatus status, Pageable pageable);

  @Query(
      """
      SELECT r.id
      FROM RecruitmentEntity r
      WHERE r.status = :entityStatus
        AND (
          (:groupId IS NOT NULL AND r.leadGroupId = :groupId)
          OR EXISTS (
            SELECT 1
            FROM RecruitmentInterviewerEntity ri
            WHERE ri.recruitmentId = r.id
              AND ri.userId = :userId
          )
          OR (
            :groupId IS NOT NULL
            AND EXISTS (
              SELECT 1
              FROM RecruitmentReferenceGroupEntity rg
              WHERE rg.recruitmentId = r.id
                AND rg.groupId = :groupId
            )
          )
        )
      """)
  List<Long> findAccessibleIdsByUserIdAndGroupIdAndStatus(
      @Param("userId") Long userId,
      @Param("groupId") Long groupId,
      @Param("entityStatus") EntityStatus entityStatus);

  @Query(
      value =
          """
          SELECT r
          FROM RecruitmentEntity r
          WHERE r.status = :entityStatus
            AND (:recruitmentStatus IS NULL OR r.recruitmentStatus = :recruitmentStatus)
            AND (
              (:groupId IS NOT NULL AND r.leadGroupId = :groupId)
              OR EXISTS (
                SELECT 1
                FROM RecruitmentInterviewerEntity ri
                WHERE ri.recruitmentId = r.id
                  AND ri.userId = :userId
              )
              OR (
                :groupId IS NOT NULL
                AND EXISTS (
                  SELECT 1
                  FROM RecruitmentReferenceGroupEntity rg
                  WHERE rg.recruitmentId = r.id
                    AND rg.groupId = :groupId
                )
              )
            )
          """,
      countQuery =
          """
          SELECT COUNT(r)
          FROM RecruitmentEntity r
          WHERE r.status = :entityStatus
            AND (:recruitmentStatus IS NULL OR r.recruitmentStatus = :recruitmentStatus)
            AND (
              (:groupId IS NOT NULL AND r.leadGroupId = :groupId)
              OR EXISTS (
                SELECT 1
                FROM RecruitmentInterviewerEntity ri
                WHERE ri.recruitmentId = r.id
                  AND ri.userId = :userId
              )
              OR (
                :groupId IS NOT NULL
                AND EXISTS (
                  SELECT 1
                  FROM RecruitmentReferenceGroupEntity rg
                  WHERE rg.recruitmentId = r.id
                    AND rg.groupId = :groupId
                )
              )
            )
          """)
  Page<RecruitmentEntity> findAccessibleByUserIdAndGroupIdAndStatus(
      @Param("userId") Long userId,
      @Param("groupId") Long groupId,
      @Param("recruitmentStatus") RecruitmentStatus recruitmentStatus,
      @Param("entityStatus") EntityStatus entityStatus,
      Pageable pageable);

  Optional<RecruitmentEntity> findByIdAndStatus(Long id, EntityStatus status);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
            UPDATE RecruitmentEntity r
               SET r.recruitmentStatus = :toStatus
             WHERE r.status = :entityStatus
               AND r.recruitmentStatus = :fromStatus
               AND r.startDate <= :currentTime
               AND r.endDate > :currentTime
            """)
  int openScheduledRecruitments(
      @Param("currentTime") LocalDateTime currentTime,
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("fromStatus") RecruitmentStatus fromStatus,
      @Param("toStatus") RecruitmentStatus toStatus);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
            UPDATE RecruitmentEntity r
               SET r.recruitmentStatus = :toStatus
             WHERE r.status = :entityStatus
               AND r.recruitmentStatus IN :fromStatuses
               AND r.endDate <= :currentTime
            """)
  int closeEndedRecruitments(
      @Param("currentTime") LocalDateTime currentTime,
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("fromStatuses") List<RecruitmentStatus> fromStatuses,
      @Param("toStatus") RecruitmentStatus toStatus);

  @Query(
      value =
          "SELECT r.tenant_id, COUNT(*) as cnt"
              + " FROM recruitments r"
              + " WHERE r.status = 'ACTIVE' AND r.recruitment_status = 'OPEN'"
              + " GROUP BY r.tenant_id",
      nativeQuery = true)
  List<Object[]> countOpenRecruitmentsByTenant();

  @Query(
      value =
          "SELECT * FROM recruitments r"
              + " WHERE r.tenant_id = :tenantId"
              + " AND r.status = 'ACTIVE'"
              + " AND r.recruitment_status = 'OPEN'"
              + " ORDER BY r.start_date DESC",
      nativeQuery = true)
  List<RecruitmentEntity> findOpenRecruitmentsByTenantId(@Param("tenantId") String tenantId);

  @Query(
      value =
          "SELECT * FROM recruitments r"
              + " WHERE r.tenant_id = :tenantId"
              + " AND r.status = 'ACTIVE'"
              + " AND r.recruitment_status = 'OPEN'"
              + " AND LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ORDER BY r.start_date DESC",
      nativeQuery = true)
  List<RecruitmentEntity> findOpenRecruitmentsByTenantIdAndSearch(
      @Param("tenantId") String tenantId, @Param("search") String search);

  @Query(
      value =
          "SELECT r.tenant_id FROM recruitments r"
              + " WHERE r.id = :recruitmentId"
              + " AND r.status = 'ACTIVE'",
      nativeQuery = true)
  Optional<String> findTenantIdById(@Param("recruitmentId") Long recruitmentId);

  long countByRecruitmentStatusAndStatus(RecruitmentStatus recruitmentStatus, EntityStatus status);

  long countByStatus(EntityStatus status);

  List<RecruitmentEntity> findByStatusOrderByCreatedAtDesc(EntityStatus status);

  @Query(
      "SELECT DISTINCT r.applicationTemplateId FROM RecruitmentEntity r"
          + " WHERE r.status = :status AND r.applicationTemplateId IN :templateIds")
  Set<Long> findUsedTemplateIds(
      @Param("templateIds") List<Long> templateIds, @Param("status") EntityStatus status);

  @Query(
      "SELECT r.applicationTemplateId, COUNT(r) FROM RecruitmentEntity r"
          + " WHERE r.status = :status AND r.applicationTemplateId IN :templateIds"
          + " GROUP BY r.applicationTemplateId")
  List<Object[]> countByTemplateIds(
      @Param("templateIds") List<Long> templateIds, @Param("status") EntityStatus status);
}
