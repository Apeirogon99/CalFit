package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomReaderImpl implements MeetingRoomReader {

  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository reservationRepository;
  private final ObjectMapper objectMapper;

  @Override
  public boolean existsByName(String name) {
    return meetingRoomRepository.existsByNameAndStatus(name, EntityStatus.ACTIVE);
  }

  @Override
  public MeetingRoom getMeetingRoom(Long meetingRoomId) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    return toMeetingRoom(entity);
  }

  @Override
  public List<MeetingRoom> getMeetingRooms() {
    return meetingRoomRepository.findAllByStatusOrderByNameAsc(EntityStatus.ACTIVE).stream()
        .map(this::toMeetingRoom)
        .toList();
  }

  @Override
  public long countOverlappingReservations(
      Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    String tenantId = requireTenantId();
    return reservationRepository
        .countByTenantIdAndMeetingRoomIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
            tenantId,
            meetingRoomId,
            List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
            endDatetime,
            startDatetime);
  }

  @Override
  public long countOverlappingReservationsByUser(
      Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    String tenantId = requireTenantId();
    return reservationRepository
        .countByTenantIdAndUserIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
            tenantId,
            userId,
            List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
            endDatetime,
            startDatetime);
  }

  @Override
  public MeetingRoomReservation getActiveReservation(Long meetingRoomId, Long reservationId) {
    String tenantId = requireTenantId();
    MeetingRoomReservationEntity entity =
        reservationRepository
            .findByTenantIdAndIdAndMeetingRoomIdAndReservationStatusIn(
                tenantId,
                reservationId,
                meetingRoomId,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE))
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    return toReservation(entity);
  }

  @Override
  public List<MeetingRoomReservation> getActiveReservations(
      LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    String tenantId = requireTenantId();
    if (fromDatetime == null || toDatetime == null || !fromDatetime.isBefore(toDatetime)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    List<Long> meetingRoomIds =
        meetingRoomRepository.findAllByStatusOrderByNameAsc(EntityStatus.ACTIVE).stream()
            .map(MeetingRoomEntity::getId)
            .toList();
    if (meetingRoomIds.isEmpty()) {
      return List.of();
    }

    return reservationRepository
        .findAllByTenantIdAndMeetingRoomIdInAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
            tenantId,
            meetingRoomIds,
            List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
            toDatetime,
            fromDatetime)
        .stream()
        .map(this::toReservation)
        .toList();
  }

  private MeetingRoom toMeetingRoom(MeetingRoomEntity entity) {
    return new MeetingRoom(
        entity.getId(),
        entity.getName(),
        entity.getLocation(),
        entity.getCapacity(),
        entity.getDescription(),
        readFacilities(entity.getFacilities()),
        entity.getColor());
  }

  private List<String> readFacilities(String rawFacilities) {
    if (rawFacilities == null || rawFacilities.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(rawFacilities, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private MeetingRoomReservation toReservation(MeetingRoomReservationEntity entity) {
    return new MeetingRoomReservation(
        entity.getId(),
        entity.getMeetingRoomId(),
        entity.getUserId(),
        entity.getInterviewScheduleId(),
        entity.getStartDatetime(),
        entity.getEndDatetime(),
        entity.getReservationStatus());
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
