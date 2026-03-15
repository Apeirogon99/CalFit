package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoomEntity, Long> {

  boolean existsByName(String name);

  boolean existsByTenantIdAndName(String tenantId, String name);

  boolean existsByNameAndStatus(String name, EntityStatus status);

  Optional<MeetingRoomEntity> findByName(String name);

  Optional<MeetingRoomEntity> findByTenantIdAndName(String tenantId, String name);

  Optional<MeetingRoomEntity> findByTenantIdAndIdAndStatus(
      String tenantId, Long id, EntityStatus status);

  List<MeetingRoomEntity> findAllByOrderByNameAsc();

  List<MeetingRoomEntity> findAllByTenantIdOrderByNameAsc(String tenantId);

  List<MeetingRoomEntity> findAllByStatusOrderByNameAsc(EntityStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  Optional<MeetingRoomEntity> findByTenantIdAndId(String tenantId, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  Optional<MeetingRoomEntity> findByIdAndStatus(Long id, EntityStatus status);
}
