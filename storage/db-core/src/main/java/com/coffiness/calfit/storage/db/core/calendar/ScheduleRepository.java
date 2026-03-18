package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

  @Query(
      "SELECT DISTINCT s FROM ScheduleEntity s "
          + "LEFT JOIN ScheduleAttendeeEntity a ON a.scheduleId = s.id "
          + "WHERE (s.userId = :userId OR a.attendeeId = :userId) "
          + "AND s.startTime <= :endDate "
          + "AND s.endTime >= :startDate "
          + "AND s.status = 'ACTIVE'")
  List<ScheduleEntity> findOverlappingSchedules(
      @Param("userId") long userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT DISTINCT s FROM ScheduleEntity s "
          + "LEFT JOIN ScheduleAttendeeEntity a ON a.scheduleId = s.id "
          + "WHERE (s.userId IN :userIds OR a.attendeeId IN :userIds) "
          + "AND s.startTime < :to "
          + "AND s.endTime > :from "
          + "AND s.status = :status "
          + "AND s.isBusy = true")
  List<ScheduleEntity> findBusyOverlappingSchedulesByUserIds(
      @Param("userIds") List<Long> userIds,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") EntityStatus status);

  // 특정 일정을 제외하고 바쁜 일정 충돌을 조회
  @Query(
      "SELECT DISTINCT s FROM ScheduleEntity s "
          + "LEFT JOIN ScheduleAttendeeEntity a ON a.scheduleId = s.id "
          + "WHERE (s.userId IN :userIds OR a.attendeeId IN :userIds) "
          + "AND s.startTime < :to "
          + "AND s.endTime > :from "
          + "AND s.status = :status "
          + "AND s.isBusy = true "
          + "AND s.id <> :excludedScheduleId")
  List<ScheduleEntity> findBusyOverlappingSchedulesByUserIdsExcludingScheduleId(
      @Param("excludedScheduleId") Long excludedScheduleId,
      @Param("userIds") List<Long> userIds,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") EntityStatus status);

  @Query(
      "SELECT s FROM ScheduleEntity s "
          + "WHERE s.roomId IN :roomIds "
          + "AND s.startTime < :to "
          + "AND s.endTime > :from "
          + "AND s.status = :status "
          + "AND s.isBusy = true")
  List<ScheduleEntity> findBusyOverlappingSchedulesByRoomIds(
      @Param("roomIds") List<Long> roomIds,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") EntityStatus status);

  Optional<ScheduleEntity> findByIdAndStatus(Long id, EntityStatus status);

  Optional<ScheduleEntity> findByGoogleEventIdAndStatus(String googleEventId, EntityStatus status);

  boolean existsByGoogleEventIdAndStatus(String googleEventId, EntityStatus status);

  List<ScheduleEntity> findAllByUserIdAndStatusOrderByStartTimeAsc(
      Long userId, EntityStatus status);

  List<ScheduleEntity> findAllByReservationIdAndStatus(Long reservationId, EntityStatus status);

  List<ScheduleEntity> findAllByReservationIdInAndStatus(
      List<Long> reservationIds, EntityStatus status);

  List<ScheduleEntity> findAllByIdInAndStatus(List<Long> scheduleIds, EntityStatus status);

  List<ScheduleEntity> findAllByInterviewScheduleIdAndStatus(
      Long interviewScheduleId, EntityStatus status);

  List<ScheduleEntity> findAllByGoogleEventIdInAndStatus(
      List<String> googleEventIds, EntityStatus status);

  @Modifying
  @Query(
      "UPDATE ScheduleEntity s "
          + "SET s.googleEventId = null "
          + "WHERE s.userId = :userId "
          + "AND s.status = :status "
          + "AND s.googleEventId IS NOT NULL")
  int clearGoogleEventIdsByUserIdAndStatus(
      @Param("userId") Long userId, @Param("status") EntityStatus status);

  long countByRoomIdAndStatusAndEndTimeAfter(Long roomId, EntityStatus status, LocalDateTime now);
}
