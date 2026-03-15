package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalCalendarRepository extends JpaRepository<ExternalCalendarEntity, Long> {

  // 특정 유저의 모든 외부 캘린더 연동 정보 조회
  List<ExternalCalendarEntity> findAllByUserIdAndStatus(Long userId, EntityStatus status);

  // 현재 테넌트에서 동기화 활성화된 외부 캘린더 목록 조회
  List<ExternalCalendarEntity> findAllByStatusAndIsSyncEnabled(
      EntityStatus status, boolean isSyncEnabled);

  Optional<ExternalCalendarEntity> findByIdAndStatus(Long id, EntityStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM ExternalCalendarEntity e WHERE e.id = :id AND e.status = :status")
  Optional<ExternalCalendarEntity> findByIdAndStatusForUpdate(
      @Param("id") Long id, @Param("status") EntityStatus status);

  // 특정 유저의 특정 외부 캘린더 단건 조회
  Optional<ExternalCalendarEntity> findByUserIdAndCalendarIdAndStatus(
      Long userId, String calendarId, EntityStatus status);
}
