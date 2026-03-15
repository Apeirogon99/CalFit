package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MeetingRoomActionType;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomStore;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomHistoryEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomHistoryRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomStoreImpl implements MeetingRoomStore {

  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository reservationRepository;
  private final MeetingRoomHistoryRepository historyRepository;
  private final ObjectMapper objectMapper;

  @Override
  public MeetingRoom create(
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    requireTenantId();
    MeetingRoomEntity entity =
        MeetingRoomEntity.builder()
            .name(name)
            .location(location)
            .capacity(capacity)
            .description(description)
            .facilities(writeFacilities(facilities))
            .color(color)
            .build();
    try {
      MeetingRoomEntity saved = meetingRoomRepository.save(entity);
      appendHistory(
          saved.getId(),
          null,
          userId,
          MeetingRoomActionType.CREATED_ROOM,
          buildRoomDetailJson(saved));
      return new MeetingRoom(
          saved.getId(),
          saved.getName(),
          saved.getLocation(),
          saved.getCapacity(),
          saved.getDescription(),
          facilities,
          saved.getColor());
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public MeetingRoom update(
      Long meetingRoomId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.update(name, location, capacity, description, writeFacilities(facilities), color);
    appendHistory(
        entity.getId(),
        null,
        userId,
        MeetingRoomActionType.UPDATED_ROOM,
        buildRoomDetailJson(entity));
    return new MeetingRoom(
        entity.getId(),
        entity.getName(),
        entity.getLocation(),
        entity.getCapacity(),
        entity.getDescription(),
        facilities,
        entity.getColor());
  }

  private String writeFacilities(List<String> facilities) {
    try {
      return objectMapper.writeValueAsString(facilities == null ? List.of() : facilities);
    } catch (JsonProcessingException exception) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  @Override
  public void delete(Long meetingRoomId, Long userId) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    String roomDetailJson = buildRoomDetailJson(entity);
    entity.archiveForDelete();
    appendHistory(entity.getId(), null, userId, MeetingRoomActionType.DELETED_ROOM, roomDetailJson);
  }

  @Override
  public MeetingRoomReservation reserve(
      Long meetingRoomId, Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    String tenantId = requireTenantId();
    syncReservationStatuses(tenantId);
    MeetingRoomReservationEntity saved =
        reservationRepository.save(
            MeetingRoomReservationEntity.builder()
                .meetingRoomId(meetingRoomId)
                .userId(userId)
                .interviewScheduleId(null)
                .startDatetime(startDatetime)
                .endDatetime(endDatetime)
                .reservationStatus(MeetingRoomStatus.RESERVED)
                .build());
    appendHistory(
        meetingRoomId,
        saved.getId(),
        userId,
        MeetingRoomActionType.RESERVED,
        buildReservationDetailJson(
            meetingRoomRepository.findById(meetingRoomId).orElse(null), saved));
    return toReservation(saved);
  }

  @Override
  public MeetingRoomReservation cancelReservation(
      Long meetingRoomId, Long reservationId, Long userId) {
    String tenantId = requireTenantId();
    syncReservationStatuses(tenantId);
    MeetingRoomReservationEntity entity =
        reservationRepository
            .findByTenantIdAndIdAndMeetingRoomIdAndReservationStatusIn(
                tenantId,
                reservationId,
                meetingRoomId,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE))
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.cancel();
    appendHistory(
        meetingRoomId,
        entity.getId(),
        userId,
        MeetingRoomActionType.CANCELED,
        buildReservationDetailJson(
            meetingRoomRepository.findById(meetingRoomId).orElse(null), entity));
    return toReservation(entity);
  }

  @Override
  public int syncReservationStatuses() {
    String tenantId = requireTenantId();
    return syncReservationStatuses(tenantId);
  }

  private void appendHistory(
      Long meetingRoomId,
      Long reservationId,
      Long userId,
      MeetingRoomActionType actionType,
      String detailJson) {
    historyRepository.save(
        MeetingRoomHistoryEntity.builder()
            .meetingRoomId(meetingRoomId)
            .meetingRoomReservationId(reservationId)
            .actorId(userId != null ? userId : 0L)
            .reason(null)
            .occurredAt(LocalDateTime.now())
            .actionType(actionType)
            .detailJson(detailJson)
            .build());
  }

  private String buildRoomDetailJson(MeetingRoomEntity room) {
    if (room == null) {
      return "{}";
    }
    return "{\"roomId\":"
        + room.getId()
        + ",\"name\":\""
        + escapeJson(room.getName())
        + "\",\"location\":"
        + room.getLocation()
        + ",\"capacity\":"
        + room.getCapacity()
        + "}";
  }

  private String buildReservationDetailJson(
      MeetingRoomEntity room, MeetingRoomReservationEntity reservation) {
    return "{"
        + "\"roomId\":"
        + reservation.getMeetingRoomId()
        + ",\"roomName\":\""
        + escapeJson(room != null ? room.getName() : "")
        + "\",\"location\":"
        + (room != null ? room.getLocation() : null)
        + ",\"capacity\":"
        + (room != null ? room.getCapacity() : null)
        + ",\"start\":\""
        + reservation.getStartDatetime()
        + "\",\"end\":\""
        + reservation.getEndDatetime()
        + "\"}";
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private int syncReservationStatuses(String tenantId) {
    LocalDateTime now = LocalDateTime.now();
    int expiredCount =
        reservationRepository.bulkUpdateToExpired(
            tenantId,
            new ArrayList<>(List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE)),
            MeetingRoomStatus.EXPIRED,
            now);
    int activeCount =
        reservationRepository.bulkUpdateToActive(
            tenantId, MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE, now);
    return expiredCount + activeCount;
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
