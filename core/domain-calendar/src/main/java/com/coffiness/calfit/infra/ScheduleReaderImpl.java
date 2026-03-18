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
  public List<Schedule> findByReservationIds(List<Long> reservationIds) {
    // 예약 ID 목록 기준 일정 일괄 조회
    List<Long> targetReservationIds = normalizeIds(reservationIds);
    if (targetReservationIds.isEmpty()) {
      return List.of();
    }

    return scheduleRepository
        .findAllByReservationIdInAndStatus(targetReservationIds, EntityStatus.ACTIVE)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Long> readAttendeeIds(Long scheduleId) {
    return readAttendeeIdsByScheduleIds(List.of(scheduleId)).getOrDefault(scheduleId, List.of());
  }

  @Override
  public Map<Long, List<Long>> readAttendeeIdsByScheduleIds(List<Long> scheduleIds) {
    // 일정 ID 목록 기준 참석자 일괄 조회
    List<Long> targetScheduleIds = normalizeIds(scheduleIds);
    if (targetScheduleIds.isEmpty()) {
      return Map.of();
    }

    return scheduleAttendeeRepository.findAllByScheduleIdIn(targetScheduleIds).stream()
        .collect(
            Collectors.groupingBy(
                ScheduleAttendeeEntity::getScheduleId,
                LinkedHashMap::new,
                Collectors.mapping(
                    ScheduleAttendeeEntity::getAttendeeId,
                    Collectors.collectingAndThen(Collectors.toList(), List::copyOf))));
  }

  @Override
  public ScheduleDetailInfo readDetail(Long scheduleId) {
    ScheduleDetailInfo detailInfo = readDetails(List.of(scheduleId)).get(scheduleId);
    if (detailInfo == null) {
      throw new IllegalArgumentException("일정을 찾을 수 없습니다.");
    }
    return detailInfo;
  }

  @Override
  public Map<Long, ScheduleDetailInfo> readDetails(List<Long> scheduleIds) {
    // 일정 상세 정보 일괄 조회
    List<Long> targetScheduleIds = normalizeIds(scheduleIds);
    if (targetScheduleIds.isEmpty()) {
      return Map.of();
    }

    List<Schedule> schedules =
        scheduleRepository.findAllByIdInAndStatus(targetScheduleIds, EntityStatus.ACTIVE).stream()
            .map(this::toDomain)
            .toList();

    return buildScheduleDetailMap(schedules);
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

    Set<Long> targetAttendeeIdSet = new LinkedHashSet<>(targetAttendeeIds);
    List<ScheduleEntity> busySchedules =
        scheduleRepository.findBusyOverlappingSchedulesByUserIds(
            targetAttendeeIds, startDate, endDate, EntityStatus.ACTIVE);

    List<Long> scheduleIds = busySchedules.stream().map(ScheduleEntity::getId).toList();
    Map<Long, List<Long>> attendeeIdsByScheduleId = readAttendeeIdsByScheduleIds(scheduleIds);
    Map<Long, List<ScheduleAvailability.BusySchedule>> busySchedulesByAttendeeId =
        new LinkedHashMap<>();

    targetAttendeeIds.forEach(
        attendeeId -> busySchedulesByAttendeeId.put(attendeeId, new ArrayList<>()));

    busySchedules.stream()
        .sorted(Comparator.comparing(ScheduleEntity::getStartTime))
        .forEach(
            schedule -> {
              ScheduleAvailability.BusySchedule busySchedule = toBusySchedule(schedule);
              LinkedHashSet<Long> visibleAttendeeIds = new LinkedHashSet<>();

              if (targetAttendeeIdSet.contains(schedule.getUserId())) {
                visibleAttendeeIds.add(schedule.getUserId());
              }

              attendeeIdsByScheduleId.getOrDefault(schedule.getId(), List.of()).stream()
                  .filter(targetAttendeeIdSet::contains)
                  .forEach(visibleAttendeeIds::add);

              visibleAttendeeIds.forEach(
                  attendeeId -> busySchedulesByAttendeeId.get(attendeeId).add(busySchedule));
            });

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
                        List.copyOf(busySchedulesByAttendeeId.getOrDefault(attendeeId, List.of()))))
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

  private Map<Long, ScheduleDetailInfo> buildScheduleDetailMap(List<Schedule> schedules) {
    // 일정 상세 조립용 연관 데이터 일괄 조회
    if (schedules == null || schedules.isEmpty()) {
      return Map.of();
    }

    List<Long> scheduleIds =
        schedules.stream().map(Schedule::id).filter(Objects::nonNull).distinct().toList();
    Map<Long, List<Long>> attendeeIdsByScheduleId = readAttendeeIdsByScheduleIds(scheduleIds);
    Map<Long, List<Long>> interviewAttendeeIdsByScheduleId =
        readInterviewAttendeeIdsByScheduleId(schedules);
    Map<Long, String> applicantNamesByInterviewScheduleId =
        readInterviewApplicantNamesByInterviewScheduleIds(
            schedules.stream()
                .map(Schedule::interviewScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

    List<Long> roomIds =
        schedules.stream().map(Schedule::roomId).filter(Objects::nonNull).distinct().toList();
    Map<Long, String> locationMap = readLocationsByRoomIds(roomIds);

    // 소유자와 참석자 이름 표시에 필요한 사용자 조회
    LinkedHashSet<Long> userIdsForLookup = new LinkedHashSet<>();
    schedules.stream()
        .map(Schedule::userId)
        .filter(Objects::nonNull)
        .forEach(userIdsForLookup::add);
    attendeeIdsByScheduleId.values().forEach(userIdsForLookup::addAll);
    interviewAttendeeIdsByScheduleId.values().forEach(userIdsForLookup::addAll);

    Map<Long, UserInfo> userMap =
        userReader.getUsers(List.copyOf(userIdsForLookup)).stream()
            .collect(Collectors.toMap(UserInfo::id, Function.identity(), (left, right) -> left));

    Map<Long, ScheduleDetailInfo> detailMap = new LinkedHashMap<>();
    for (Schedule schedule : schedules) {
      List<Long> attendeeIds =
          schedule.interviewScheduleId() != null
              ? interviewAttendeeIdsByScheduleId.getOrDefault(schedule.id(), List.of())
              : attendeeIdsByScheduleId.getOrDefault(schedule.id(), List.of());

      List<String> attendees =
          attendeeIds.stream()
              .map(attendeeId -> resolveUserName(userMap.get(attendeeId), attendeeId))
              .toList();

      String ownerName = resolveUserName(userMap.get(schedule.userId()), schedule.userId());
      String location =
          schedule.roomId() == null
              ? "지정한 장소 없음"
              : locationMap.getOrDefault(schedule.roomId(), "알 수 없는 장소");
      String applicantName =
          schedule.interviewScheduleId() == null
              ? null
              : applicantNamesByInterviewScheduleId.get(schedule.interviewScheduleId());

      detailMap.put(
          schedule.id(),
          ScheduleDetailInfo.of(
              schedule, location, ownerName, attendeeIds, attendees, applicantName));
    }

    return detailMap;
  }

  private Map<Long, List<Long>> readInterviewAttendeeIdsByScheduleId(List<Schedule> schedules) {
    // 면접 일정 기준 면접관 참석자 조회
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || schedules == null || schedules.isEmpty()) {
      return Map.of();
    }

    List<Long> interviewScheduleIds =
        schedules.stream()
            .map(Schedule::interviewScheduleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (interviewScheduleIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, List<Long>> interviewerIdsByInterviewScheduleId =
        interviewScheduleInterviewerRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, interviewScheduleIds)
            .stream()
            .filter(entity -> entity.getInterviewScheduleId() != null && entity.getUserId() != null)
            .collect(
                Collectors.groupingBy(
                    InterviewScheduleInterviewerEntity::getInterviewScheduleId,
                    LinkedHashMap::new,
                    Collectors.mapping(
                        InterviewScheduleInterviewerEntity::getUserId,
                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf))));

    Map<Long, List<Long>> attendeeIdsByScheduleId = new LinkedHashMap<>();
    for (Schedule schedule : schedules) {
      if (schedule.interviewScheduleId() == null) {
        continue;
      }

      List<Long> attendeeIds =
          interviewerIdsByInterviewScheduleId
              .getOrDefault(schedule.interviewScheduleId(), List.of())
              .stream()
              .filter(userId -> !userId.equals(schedule.userId()))
              .distinct()
              .toList();
      attendeeIdsByScheduleId.put(schedule.id(), attendeeIds);
    }

    return attendeeIdsByScheduleId;
  }

  private Map<Long, String> readInterviewApplicantNamesByInterviewScheduleIds(
      List<Long> interviewScheduleIds) {
    // 면접 일정 기준 지원자 이름 조회
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      return Map.of();
    }

    List<Long> targetInterviewScheduleIds = normalizeIds(interviewScheduleIds);
    if (targetInterviewScheduleIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, List<Long>> applicantIdsByInterviewScheduleId =
        interviewScheduleApplicantRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, targetInterviewScheduleIds)
            .stream()
            .filter(
                entity ->
                    entity.getInterviewScheduleId() != null && entity.getApplicantId() != null)
            .collect(
                Collectors.groupingBy(
                    InterviewScheduleApplicantEntity::getInterviewScheduleId,
                    LinkedHashMap::new,
                    Collectors.mapping(
                        InterviewScheduleApplicantEntity::getApplicantId,
                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf))));

    List<Long> applicantIds =
        applicantIdsByInterviewScheduleId.values().stream()
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (applicantIds.isEmpty()) {
      return Map.of();
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

    Map<Long, String> applicantNamesByInterviewScheduleId = new LinkedHashMap<>();
    for (Long interviewScheduleId : targetInterviewScheduleIds) {
      String applicantNames =
          applicantIdsByInterviewScheduleId.getOrDefault(interviewScheduleId, List.of()).stream()
              .map(applicantId -> applicantNameMap.getOrDefault(applicantId, "지원자#" + applicantId))
              .collect(Collectors.joining(", "));

      if (!applicantNames.isBlank()) {
        applicantNamesByInterviewScheduleId.put(interviewScheduleId, applicantNames);
      }
    }

    return applicantNamesByInterviewScheduleId;
  }

  private List<Long> normalizeIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }

    return ids.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
  }

  private ScheduleAvailability.BusySchedule toBusySchedule(ScheduleEntity schedule) {
    return new ScheduleAvailability.BusySchedule(
        schedule.getId(),
        schedule.getTitle(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.isAllDay(),
        schedule.getType(),
        schedule.getInterviewScheduleId());
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
