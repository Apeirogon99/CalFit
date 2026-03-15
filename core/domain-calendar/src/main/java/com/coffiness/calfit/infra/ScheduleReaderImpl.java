package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.Schedule;
import com.coffiness.calfit.domain.ScheduleAvailability;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleReader;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleReaderImpl implements ScheduleReader {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleAttendeeRepository scheduleAttendeeRepository;
  private final MeetingRoomRepository meetingRoomRepository;
  private final UserReader userReader;
  private final InterviewScheduleInterviewerRepository interviewScheduleInterviewerRepository;
  private final InterviewScheduleApplicantRepository interviewScheduleApplicantRepository;
  private final ApplicantRepository applicantRepository;

  @Override
  public List<Schedule> findOverlappingSchedules(
      Long userId, LocalDateTime startDate, LocalDateTime endDate) {
    return scheduleRepository.findOverlappingSchedules(userId, startDate, endDate).stream()
        .map(this::toDomain)
        .toList();
  }

  // 바쁜 일정 충돌 여부를 조회
  @Override
  public boolean existsBusyOverlappingScheduleConflict(
      List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate) {
    if (userIds == null || userIds.isEmpty()) {
      return false;
    }

    return !scheduleRepository
        .findBusyOverlappingSchedulesByUserIds(userIds, startDate, endDate, EntityStatus.ACTIVE)
        .isEmpty();
  }

  // 특정 일정을 제외하고 바쁜 일정 충돌 여부를 조회
  @Override
  public boolean existsBusyOverlappingScheduleConflictExcludingSchedule(
      Long excludedScheduleId, List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate) {
    if (excludedScheduleId == null || userIds == null || userIds.isEmpty()) {
      return false;
    }

    return !scheduleRepository
        .findBusyOverlappingSchedulesByUserIdsExcludingScheduleId(
            excludedScheduleId, userIds, startDate, endDate, EntityStatus.ACTIVE)
        .isEmpty();
  }

  @Override
  public Schedule read(Long scheduleId) {
    ScheduleEntity entity =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
    return toDomain(entity);
  }

  @Override
  public Schedule readByGoogleEventId(String googleEventId) {
    return scheduleRepository
        .findByGoogleEventIdAndStatus(googleEventId, EntityStatus.ACTIVE)
        .map(this::toDomain)
        .orElse(null);
  }

  // 사용자가 소유한 활성 일정 전체를 조회
  @Override
  public List<Schedule> readAllOwnedSchedules(Long userId) {
    if (userId == null) {
      return List.of();
    }

    return scheduleRepository
        .findAllByUserIdAndStatusOrderByStartTimeAsc(userId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Map<Long, String> readLocationsByRoomIds(List<Long> roomIds) {
    if (roomIds == null || roomIds.isEmpty()) {
      return Map.of();
    }

    List<Long> targetRoomIds =
        roomIds.stream().filter(Objects::nonNull).filter(roomId -> roomId > 0).distinct().toList();

    if (targetRoomIds.isEmpty()) {
      return Map.of();
    }

    return meetingRoomRepository.findAllById(targetRoomIds).stream()
        .collect(
            Collectors.toMap(
                MeetingRoomEntity::getId, MeetingRoomEntity::getName, (left, right) -> left));
  }

  @Override
  public List<Schedule> findByReservationId(Long reservationId) {
    if (reservationId == null) {
      return List.of();
    }

    return scheduleRepository
        .findAllByReservationIdAndStatus(reservationId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Long> readAttendeeIds(Long scheduleId) {
    return scheduleAttendeeRepository.findByScheduleId(scheduleId).stream()
        .map(ScheduleAttendeeEntity::getAttendeeId)
        .toList();
  }

  @Override
  public ScheduleDetailInfo readDetail(Long scheduleId) {
    Schedule schedule = read(scheduleId);
    List<Long> attendeeIds = readAttendeeIds(scheduleId);
    String applicantName = null;

    if (schedule.interviewScheduleId() != null) {
      attendeeIds = readInterviewAttendeeIds(schedule);
      applicantName = readInterviewApplicantName(schedule.interviewScheduleId());
    }

    String location = "지정한 장소 없음";
    if (schedule.roomId() != null) {
      location =
          meetingRoomRepository
              .findById(schedule.roomId())
              .map(MeetingRoomEntity::getName)
              .orElse("알 수 없는 장소");
    }

    List<Long> userIdsForLookup =
        Stream.concat(Stream.of(schedule.userId()), attendeeIds.stream()).distinct().toList();
    Map<Long, UserInfo> userMap =
        userReader.getUsers(userIdsForLookup).stream()
            .collect(Collectors.toMap(UserInfo::id, Function.identity(), (left, right) -> left));

    List<String> attendees =
        attendeeIds.stream()
            .map(attendeeId -> resolveUserName(userMap.get(attendeeId), attendeeId))
            .toList();

    String ownerName = resolveUserName(userMap.get(schedule.userId()), schedule.userId());

    return ScheduleDetailInfo.of(
        schedule, location, ownerName, attendeeIds, attendees, applicantName);
  }

  // 선택한 참석자들의 바쁜 일정 현황을 조회
  @Override
  public ScheduleAvailability readAttendeeAvailability(
      List<Long> attendeeIds, LocalDateTime startDate, LocalDateTime endDate) {
    if (attendeeIds == null || attendeeIds.isEmpty()) {
      return ScheduleAvailability.empty();
    }

    List<Long> targetAttendeeIds =
        attendeeIds.stream()
            .filter(Objects::nonNull)
            .filter(attendeeId -> attendeeId > 0)
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toCollection(LinkedHashSet::new), List::copyOf));

    if (targetAttendeeIds.isEmpty()) {
      return ScheduleAvailability.empty();
    }

    List<ScheduleEntity> busySchedules =
        scheduleRepository.findBusyOverlappingSchedulesByUserIds(
            targetAttendeeIds, startDate, endDate, EntityStatus.ACTIVE);

    List<Long> scheduleIds = busySchedules.stream().map(ScheduleEntity::getId).toList();
    Map<Long, List<Long>> attendeeIdsByScheduleId =
        scheduleIds.isEmpty()
            ? Map.of()
            : scheduleAttendeeRepository.findAllByScheduleIdIn(scheduleIds).stream()
                .collect(
                    Collectors.groupingBy(
                        ScheduleAttendeeEntity::getScheduleId,
                        Collectors.mapping(
                            ScheduleAttendeeEntity::getAttendeeId, Collectors.toList())));

    Map<Long, UserInfo> userMap =
        userReader.getUsers(targetAttendeeIds).stream()
            .collect(Collectors.toMap(UserInfo::id, Function.identity(), (left, right) -> left));

    List<ScheduleAvailability.AttendeeAvailability> attendeeAvailabilities =
        targetAttendeeIds.stream()
            .map(
                attendeeId ->
                    new ScheduleAvailability.AttendeeAvailability(
                        attendeeId,
                        resolveUserName(userMap.get(attendeeId), attendeeId),
                        busySchedules.stream()
                            .filter(
                                schedule ->
                                    isScheduleVisibleToAttendee(
                                        attendeeId,
                                        schedule,
                                        attendeeIdsByScheduleId.getOrDefault(
                                            schedule.getId(), List.of())))
                            .sorted(Comparator.comparing(ScheduleEntity::getStartTime))
                            .map(
                                schedule ->
                                    new ScheduleAvailability.BusySchedule(
                                        schedule.getId(),
                                        schedule.getTitle(),
                                        schedule.getStartTime(),
                                        schedule.getEndTime(),
                                        schedule.isAllDay(),
                                        schedule.getType(),
                                        schedule.getInterviewScheduleId()))
                            .toList()))
            .toList();

    return new ScheduleAvailability(attendeeAvailabilities);
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

  // 면접 연동 일정이면 함께 참여하는 다른 면접관 ID를 조회
  private List<Long> readInterviewAttendeeIds(Schedule schedule) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || schedule.interviewScheduleId() == null) {
      return List.of();
    }

    return interviewScheduleInterviewerRepository
        .findAllByTenantIdAndInterviewScheduleIdIn(
            tenantId, List.of(schedule.interviewScheduleId()))
        .stream()
        .map(InterviewScheduleInterviewerEntity::getUserId)
        .filter(userId -> userId != null && !userId.equals(schedule.userId()))
        .distinct()
        .toList();
  }

  // 면접 연동 일정이면 지원자 이름을 조회
  private String readInterviewApplicantName(Long interviewScheduleId) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || interviewScheduleId == null) {
      return null;
    }

    List<Long> applicantIds =
        interviewScheduleApplicantRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, List.of(interviewScheduleId))
            .stream()
            .map(InterviewScheduleApplicantEntity::getApplicantId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (applicantIds.isEmpty()) {
      return null;
    }

    Map<Long, String> applicantNameMap =
        applicantRepository.findByTenantIdAndIdIn(tenantId, applicantIds).stream()
            .filter(applicant -> applicant.getId() != null)
            .collect(
                Collectors.toMap(
                    applicant -> applicant.getId(),
                    applicant -> fallbackApplicantName(applicant.getName(), applicant.getId()),
                    (left, right) -> left,
                    LinkedHashMap::new));

    return applicantIds.stream()
        .map(applicantId -> applicantNameMap.getOrDefault(applicantId, "지원자#" + applicantId))
        .collect(Collectors.joining(", "));
  }

  // 참석자에게 해당 일정이 실제로 보이는지 확인
  private boolean isScheduleVisibleToAttendee(
      Long attendeeId, ScheduleEntity schedule, List<Long> attendeeIds) {
    return Objects.equals(schedule.getUserId(), attendeeId) || attendeeIds.contains(attendeeId);
  }

  // 참석자 이름을 응답용 문자열로 변환
  private String resolveUserName(UserInfo userInfo, Long attendeeId) {
    if (userInfo == null || userInfo.name() == null || userInfo.name().isBlank()) {
      return "이름 없는 사용자 (" + attendeeId + ")";
    }
    return userInfo.name();
  }

  // 지원자 이름이 비어 있으면 기본 표시명을 반환
  private String fallbackApplicantName(String applicantName, Long applicantId) {
    if (applicantName == null || applicantName.isBlank()) {
      return "지원자#" + applicantId;
    }
    return applicantName;
  }
}
