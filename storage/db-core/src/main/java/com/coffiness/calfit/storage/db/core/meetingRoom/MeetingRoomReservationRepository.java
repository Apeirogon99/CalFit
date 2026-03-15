package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRoomReservationRepository
    extends JpaRepository<MeetingRoomReservationEntity, Long> {
  long
      countByTenantIdAndMeetingRoomIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
          String tenantId,
          Long meetingRoomId,
          List<MeetingRoomStatus> reservationStatuses,
          LocalDateTime endTime,
          LocalDateTime startTime);

  long countByTenantIdAndUserIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
      String tenantId,
      Long userId,
      List<MeetingRoomStatus> reservationStatuses,
      LocalDateTime endTime,
      LocalDateTime startTime);

  List<MeetingRoomReservationEntity>
      findAllByTenantIdAndMeetingRoomIdInAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
          String tenantId,
          List<Long> meetingRoomIds,
          List<MeetingRoomStatus> reservationStatuses,
          LocalDateTime to,
          LocalDateTime from);

  Optional<MeetingRoomReservationEntity> findByTenantIdAndIdAndMeetingRoomIdAndReservationStatusIn(
      String tenantId, Long id, Long meetingRoomId, List<MeetingRoomStatus> reservationStatuses);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE MeetingRoomReservationEntity r "
          + "SET r.reservationStatus = :nextStatus "
          + "WHERE r.tenantId = :tenantId "
          + "AND r.reservationStatus IN :currentStatuses "
          + "AND r.endDatetime <= :now")
  int bulkUpdateToExpired(
      @Param("tenantId") String tenantId,
      @Param("currentStatuses") List<MeetingRoomStatus> currentStatuses,
      @Param("nextStatus") MeetingRoomStatus nextStatus,
      @Param("now") LocalDateTime now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE MeetingRoomReservationEntity r "
          + "SET r.reservationStatus = :nextStatus "
          + "WHERE r.tenantId = :tenantId "
          + "AND r.reservationStatus = :currentStatus "
          + "AND r.startDatetime <= :now "
          + "AND r.endDatetime > :now")
  int bulkUpdateToActive(
      @Param("tenantId") String tenantId,
      @Param("currentStatus") MeetingRoomStatus currentStatus,
      @Param("nextStatus") MeetingRoomStatus nextStatus,
      @Param("now") LocalDateTime now);

  long countByTenantIdAndMeetingRoomIdAndReservationStatusInAndEndDatetimeAfter(
      String tenantId,
      Long meetingRoomId,
      List<MeetingRoomStatus> reservationStatuses,
      LocalDateTime now);
}
