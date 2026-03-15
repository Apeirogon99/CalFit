package com.coffiness.calfit.storage.db.core.notification;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  Page<NotificationEntity> findByTenantIdAndRecipientUserIdAndStatusOrderByCreatedAtDesc(
      String tenantId, Long recipientUserId, EntityStatus status, Pageable pageable);

  Page<NotificationEntity> findByTenantIdAndRecipientUserIdAndStatusAndTypeInOrderByCreatedAtDesc(
      String tenantId,
      Long recipientUserId,
      EntityStatus status,
      List<NotificationType> types,
      Pageable pageable);

  Page<NotificationEntity>
      findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
          String tenantId, Long recipientUserId, EntityStatus status, Pageable pageable);

  Page<NotificationEntity>
      findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseAndTypeInOrderByCreatedAtDesc(
          String tenantId,
          Long recipientUserId,
          EntityStatus status,
          List<NotificationType> types,
          Pageable pageable);

  Optional<NotificationEntity> findByTenantIdAndRecipientUserIdAndIdAndStatus(
      String tenantId, Long recipientUserId, Long id, EntityStatus status);

  List<NotificationEntity>
      findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
          String tenantId, Long recipientUserId, EntityStatus status);

  long countByTenantIdAndRecipientUserIdAndStatusAndIsReadFalse(
      String tenantId, Long recipientUserId, EntityStatus status);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update NotificationEntity n "
          + "set n.status = :deletedStatus "
          + "where n.tenantId = :tenantId "
          + "and n.recipientUserId = :recipientUserId "
          + "and n.status = :activeStatus")
  int updateStatusByTenantIdAndRecipientUserIdAndStatus(
      @Param("tenantId") String tenantId,
      @Param("recipientUserId") Long recipientUserId,
      @Param("activeStatus") EntityStatus activeStatus,
      @Param("deletedStatus") EntityStatus deletedStatus);
}
