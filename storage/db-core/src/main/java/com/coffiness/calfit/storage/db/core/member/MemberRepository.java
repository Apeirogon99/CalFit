package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

  List<MemberEntity> findByStatus(EntityStatus status);

  @Query(
      value =
          "SELECT tenant_id FROM workspace_members WHERE user_id = :userId AND status = 'ACTIVE' LIMIT 1",
      nativeQuery = true)
  Optional<String> findTenantIdByUserId(@Param("userId") Long userId);

  MemberEntity findByTenantIdAndUserId(String tenantId, Long userId);

  MemberEntity findByTenantIdAndUserIdAndStatus(String tenantId, Long userId, EntityStatus status);

  boolean existsByTenantIdAndUserIdAndStatus(String workspaceId, Long userId, EntityStatus status);

  List<MemberEntity> findByGroupIdAndStatus(Long groupId, EntityStatus status);

  // 읽기 전용 멤버 검증에서 사용하는 잠금 없는 조회
  List<MemberEntity> findAllByTenantIdAndUserIdIn(String tenantId, List<Long> userIds);

  // 쓰기 시점 정합성 확인이 필요할 때 사용하는 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM MemberEntity m WHERE m.tenantId = :tenantId AND m.userId IN :userIds")
  List<MemberEntity> findAllByTenantIdAndUserIdInForUpdate(
      @Param("tenantId") String tenantId, @Param("userIds") List<Long> userIds);

  @Query(
      "SELECT m.groupId, COUNT(m) FROM MemberEntity m"
          + " WHERE m.groupId IN :groupIds AND m.status = :status GROUP BY m.groupId")
  List<Object[]> countByGroupIdsAndStatus(
      @Param("groupIds") List<Long> groupIds, @Param("status") EntityStatus status);

  long countByTenantIdAndStatus(String tenantId, EntityStatus status);

  long countByTenantIdAndMemberTypeAndStatus(
      String tenantId, com.coffiness.calfit.core.enums.MemberType memberType, EntityStatus status);
}
