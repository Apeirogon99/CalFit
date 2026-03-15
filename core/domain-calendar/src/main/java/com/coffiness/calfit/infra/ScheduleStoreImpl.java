package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.Schedule;
import com.coffiness.calfit.domain.ScheduleStore;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleStoreImpl implements ScheduleStore {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleAttendeeRepository scheduleAttendeeRepository;

  @Override
  public Schedule store(Schedule schedule, List<Long> attendeeIds) {
    ScheduleEntity entity =
        ScheduleEntity.builder()
            .userId(schedule.userId())
            .title(schedule.title())
            .description(schedule.description())
            .startTime(schedule.startTime())
            .endTime(schedule.endTime())
            .isAllDay(schedule.isAllDay())
            .roomId(schedule.roomId())
            .reservationId(schedule.reservationId())
            .type(schedule.type())
            .isBusy(schedule.isBusy())
            .googleEventId(schedule.googleEventId())
            .interviewScheduleId(schedule.interviewScheduleId())
            .build();

    ScheduleEntity savedEntity = scheduleRepository.save(entity);

    if (attendeeIds != null && !attendeeIds.isEmpty()) {
      List<ScheduleAttendeeEntity> attendees =
          attendeeIds.stream()
              .map(
                  attendeeId ->
                      ScheduleAttendeeEntity.builder()
                          .scheduleId(savedEntity.getId())
                          .attendeeId(attendeeId)
                          .build())
              .toList();
      scheduleAttendeeRepository.saveAll(attendees);
    }

    return toDomain(savedEntity);
  }

  @Override
  public void update(Schedule schedule, List<Long> attendeeIds) {
    ScheduleEntity entity =
        scheduleRepository
            .findByIdAndStatus(schedule.id(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    entity.update(
        schedule.title(),
        schedule.description(),
        schedule.type(),
        schedule.startTime(),
        schedule.endTime(),
        schedule.isAllDay(),
        schedule.roomId(),
        schedule.reservationId(),
        schedule.isBusy());

    scheduleAttendeeRepository.deleteByScheduleId(schedule.id());

    if (attendeeIds != null && !attendeeIds.isEmpty()) {
      List<ScheduleAttendeeEntity> newAttendees =
          attendeeIds.stream()
              .map(
                  attendeeId ->
                      ScheduleAttendeeEntity.builder()
                          .scheduleId(schedule.id())
                          .attendeeId(attendeeId)
                          .build())
              .toList();
      scheduleAttendeeRepository.saveAllAndFlush(newAttendees);
    }
  }

  @Override
  @Transactional
  public void updateGoogleEventId(Long scheduleId, String googleEventId) {
    if (!hasText(googleEventId)) {
      throw new IllegalArgumentException("GOOGLE_EVENT_ID_REQUIRED");
    }

    ScheduleEntity entity =
        scheduleRepository
            .findByIdAndStatus(scheduleId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    String currentGoogleEventId = entity.getGoogleEventId();

    if (!hasText(currentGoogleEventId)) {
      entity.updateGoogleEventId(googleEventId);
      return;
    }

    if (currentGoogleEventId.equals(googleEventId)) {
      return;
    }

    throw new IllegalStateException("GOOGLE_EVENT_ID_CONFLICT");
  }

  @Override
  @Transactional
  public void clearGoogleEventIdsByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("USER_ID_REQUIRED");
    }

    scheduleRepository.clearGoogleEventIdsByUserIdAndStatus(userId, EntityStatus.ACTIVE);
  }

  @Override
  public void delete(Schedule schedule) {
    ScheduleEntity entity =
        scheduleRepository
            .findByIdAndStatus(schedule.id(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    scheduleAttendeeRepository.deleteByScheduleId(schedule.id());
    entity.updateGoogleEventId(null);
    entity.deleted();
  }

  @Override
  public void deleteByReservationId(Long reservationId) {
    if (reservationId == null) {
      return;
    }

    List<ScheduleEntity> schedules =
        scheduleRepository.findAllByReservationIdAndStatus(reservationId, EntityStatus.ACTIVE);
    for (ScheduleEntity schedule : schedules) {
      scheduleAttendeeRepository.deleteByScheduleId(schedule.getId());
      schedule.updateGoogleEventId(null);
      schedule.deleted();
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private Schedule toDomain(ScheduleEntity entity) {
    return new Schedule(
        entity.getId(),
        entity.getUserId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getType(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.isAllDay(),
        entity.getRoomId(),
        entity.getReservationId(),
        entity.isBusy(),
        entity.getGoogleEventId(),
        entity.getInterviewScheduleId());
  }
}
