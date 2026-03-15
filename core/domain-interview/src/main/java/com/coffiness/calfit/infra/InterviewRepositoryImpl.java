package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.*;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewRepositoryImpl implements InterviewRepository {

  private final MemberReader memberReader;
  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository meetingRoomReservationRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final InterviewScheduleHistoryRepository interviewScheduleHistoryRepository;
  private final InterviewScheduleInterviewerRepository interviewerRepository;
  private final InterviewScheduleApplicantRepository applicantMappingRepository;
  private final ScheduleRepository scheduleRepository;
  private final UserRepository userRepository;
  private final ApplicantRepository applicantRepository;
  private final ApplicationRepository applicationRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;

  @Override
  public boolean isHrMember(Long userId) {
    if (userId == null) {
      return false;
    }
    String tenantId = requireTenantId();
    Member member = memberReader.getMember(tenantId, userId);
    return member != null && member.memberType() == MemberType.HR;
  }

  @Override
  public int getMeetingRoomCapacity(Long meetingRoomId) {
    String tenantId = requireTenantId();
    return meetingRoomRepository
        .findByTenantIdAndIdAndStatus(tenantId, meetingRoomId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND))
        .getCapacity();
  }

  @Override
  public List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    if (meetingRoomIds == null || meetingRoomIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<MeetingRoomBusySlotRow> result =
        meetingRoomReservationRepository
            .findAllByTenantIdAndMeetingRoomIdInAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId,
                meetingRoomIds,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
                to,
                from)
            .stream()
            .map(
                reservation ->
                    new MeetingRoomBusySlotRow(
                        reservation.getMeetingRoomId(),
                        reservation.getInterviewScheduleId() != null
                            ? reservation.getInterviewScheduleId()
                            : reservation.getId(),
                        reservation.getStartDatetime(),
                        reservation.getEndDatetime()))
            .toList();

    List<ScheduleEntity> calendarBusySchedules =
        scheduleRepository.findBusyOverlappingSchedulesByRoomIds(
            meetingRoomIds, from, to, EntityStatus.ACTIVE);

    List<MeetingRoomBusySlotRow> merged = new ArrayList<>(result);
    for (ScheduleEntity schedule : calendarBusySchedules) {
      if (schedule.getInterviewScheduleId() != null) {
        continue;
      }
      merged.add(
          new MeetingRoomBusySlotRow(
              schedule.getRoomId(),
              schedule.getId(),
              schedule.getStartTime(),
              schedule.getEndTime()));
    }
    return merged;
  }

  @Override
  public List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleInterviewerEntity> mappings =
        interviewerRepository.findAllByTenantIdAndUserIdIn(tenantId, interviewerIds);

    Map<String, InterviewerBusySlotRow> uniqueResult = new HashMap<>();

    Set<Long> scheduleIds =
        mappings.stream()
            .map(InterviewScheduleInterviewerEntity::getInterviewScheduleId)
            .collect(Collectors.toSet());
    if (!scheduleIds.isEmpty()) {
      List<InterviewScheduleEntity> schedules =
          interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
              tenantId, new ArrayList<>(scheduleIds), InterviewStatus.CANCELLED, to);

      Map<Long, InterviewScheduleEntity> scheduleMap = new HashMap<>();
      for (InterviewScheduleEntity schedule : schedules) {
        LocalDateTime start = schedule.getScheduledAt();
        LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
        if (start.isBefore(to) && from.isBefore(end)) {
          scheduleMap.put(schedule.getId(), schedule);
        }
      }

      for (InterviewScheduleInterviewerEntity mapping : mappings) {
        InterviewScheduleEntity schedule = scheduleMap.get(mapping.getInterviewScheduleId());
        if (schedule == null) {
          continue;
        }

        LocalDateTime start = schedule.getScheduledAt();
        LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
        String key = mapping.getUserId() + "|" + schedule.getId() + "|" + start + "|" + end;
        uniqueResult.putIfAbsent(
            key, new InterviewerBusySlotRow(mapping.getUserId(), schedule.getId(), start, end));
      }
    }

    List<Long> normalizedInterviewerIds =
        interviewerIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    for (Long interviewerId : normalizedInterviewerIds) {
      List<ScheduleEntity> visibleSchedules =
          scheduleRepository.findOverlappingSchedules(interviewerId, from, to);

      for (ScheduleEntity schedule : visibleSchedules) {
        if (!schedule.isActive()
            || !schedule.isBusy()
            || schedule.getInterviewScheduleId() != null) {
          continue;
        }

        String key =
            interviewerId
                + "|"
                + schedule.getId()
                + "|"
                + schedule.getStartTime()
                + "|"
                + schedule.getEndTime();
        uniqueResult.putIfAbsent(
            key,
            new InterviewerBusySlotRow(
                interviewerId, schedule.getId(), schedule.getStartTime(), schedule.getEndTime()));
      }
    }

    return uniqueResult.values().stream()
        .sorted(
            Comparator.comparing(InterviewerBusySlotRow::start)
                .thenComparing(InterviewerBusySlotRow::interviewerId)
                .thenComparing(InterviewerBusySlotRow::interviewScheduleId))
        .toList();
  }

  @Override
  public List<ApplicantBusySlotRow> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleApplicantEntity> mappings =
        applicantMappingRepository.findAllByTenantIdAndApplicantIdIn(tenantId, applicantIds);

    Set<Long> scheduleIds =
        mappings.stream()
            .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
            .collect(Collectors.toSet());
    if (scheduleIds.isEmpty()) {
      return List.of();
    }

    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, new ArrayList<>(scheduleIds), InterviewStatus.CANCELLED, to);

    Map<Long, InterviewScheduleEntity> scheduleMap = new HashMap<>();
    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      if (start.isBefore(to) && from.isBefore(end)) {
        scheduleMap.put(schedule.getId(), schedule);
      }
    }

    List<ApplicantBusySlotRow> result = new ArrayList<>();
    for (InterviewScheduleApplicantEntity mapping : mappings) {
      InterviewScheduleEntity schedule = scheduleMap.get(mapping.getInterviewScheduleId());
      if (schedule == null) {
        continue;
      }
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      result.add(new ApplicantBusySlotRow(mapping.getApplicantId(), schedule.getId(), start, end));
    }
    return result;
  }

  @Override
  public List<InterviewScheduleCalendarRow> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to) {
    if (recruitmentId == null || from == null || to == null || !from.isBefore(to)) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository
            .findAllByTenantIdAndRecruitmentIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNotOrderByScheduledAtAsc(
                tenantId, recruitmentId, from, to, InterviewStatus.CANCELLED);

    if (schedules.isEmpty()) {
      return List.of();
    }

    List<Long> scheduleIds = schedules.stream().map(InterviewScheduleEntity::getId).toList();

    Map<Long, List<InterviewScheduleInterviewerEntity>> interviewersByScheduleId =
        interviewerRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleInterviewerEntity::getInterviewScheduleId));

    Map<Long, List<InterviewScheduleApplicantEntity>> applicantsByScheduleId =
        applicantMappingRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleApplicantEntity::getInterviewScheduleId));

    List<Long> interviewerIds =
        interviewersByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleInterviewerEntity::getUserId)
            .distinct()
            .toList();
    Map<Long, String> interviewerNameMap = resolveInterviewerNameMap(interviewerIds);

    List<Long> applicantIds =
        applicantsByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleApplicantEntity::getApplicantId)
            .distinct()
            .toList();
    Map<Long, String> applicantNameMap = resolveApplicantNameMap(tenantId, applicantIds);

    Map<Long, String> recruitmentTitleMap = resolveRecruitmentTitleMap(List.of(recruitmentId));

    List<Long> recruitmentStageIds =
        schedules.stream()
            .map(InterviewScheduleEntity::getRecruitmentStageId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    Map<Long, String> stageNameMap = resolveStageNameMap(recruitmentStageIds);
    List<Long> meetingRoomIds =
        schedules.stream()
            .map(InterviewScheduleEntity::getMeetingRoomId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    Map<Long, String> locationMap = resolveMeetingRoomLocationMap(meetingRoomIds);

    List<InterviewScheduleCalendarRow> result = new ArrayList<>();
    for (InterviewScheduleEntity schedule : schedules) {
      List<InterviewScheduleInterviewerEntity> interviewerMappings =
          interviewersByScheduleId.getOrDefault(schedule.getId(), List.of());
      List<InterviewScheduleApplicantEntity> applicantMappings =
          applicantsByScheduleId.getOrDefault(schedule.getId(), List.of());

      Long interviewerUserId =
          interviewerMappings.isEmpty() ? null : interviewerMappings.get(0).getUserId();

      String interviewerName =
          interviewerMappings.stream()
              .map(InterviewScheduleInterviewerEntity::getUserId)
              .map(id -> fallbackName(interviewerNameMap.get(id), "면접관#" + id))
              .distinct()
              .collect(Collectors.joining(", "));

      String applicantName =
          applicantMappings.stream()
              .map(InterviewScheduleApplicantEntity::getApplicantId)
              .map(id -> fallbackName(applicantNameMap.get(id), "지원자#" + id))
              .distinct()
              .collect(Collectors.joining(", "));

      result.add(
          new InterviewScheduleCalendarRow(
              schedule.getId(),
              schedule.getScheduledAt(),
              schedule.getEndTime(),
              schedule.getMeetingRoomId(),
              interviewerUserId,
              interviewerName,
              buildWeeklyScheduleTitle(
                  recruitmentTitleMap.get(schedule.getRecruitmentId()),
                  stageNameMap.get(schedule.getRecruitmentStageId())),
              applicantName,
              fallbackName(schedule.getMemo(), ""),
              fallbackName(locationMap.get(schedule.getMeetingRoomId()), "")));
    }
    return result;
  }

  // 채용 공고의 면접 단계별 대기 지원자 목록을 조회
  @Override
  public List<PendingInterviewStageRow> getPendingInterviewStages(Long recruitmentId) {
    if (recruitmentId == null) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<RecruitmentStageEntity> interviewStages =
        recruitmentStageRepository.findByRecruitmentIdOrderByStageStepAsc(recruitmentId).stream()
            .filter(RecruitmentStageEntity::isActive)
            .filter(stage -> stage.getStageType() == RecruitmentStageType.INTERVIEW)
            .toList();
    if (interviewStages.isEmpty()) {
      return List.of();
    }

    Set<Long> interviewStageIds =
        interviewStages.stream().map(RecruitmentStageEntity::getId).collect(Collectors.toSet());

    Map<Long, List<ApplicationEntity>> applicationsByStageId =
        applicationRepository
            .findByRecruitmentIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .stream()
            .filter(
                application -> interviewStageIds.contains(application.getRecruitmentProcessId()))
            .collect(Collectors.groupingBy(ApplicationEntity::getRecruitmentProcessId));

    Map<Long, Set<Long>> scheduledApplicantIdsByStageId =
        getScheduledApplicantIdsByStageId(tenantId, recruitmentId, interviewStageIds);

    List<PendingInterviewStageRow> result = new ArrayList<>();
    for (RecruitmentStageEntity stage : interviewStages) {
      Set<Long> scheduledApplicantIds =
          scheduledApplicantIdsByStageId.getOrDefault(stage.getId(), Set.of());

      List<PendingInterviewApplicantRow> applicants =
          applicationsByStageId.getOrDefault(stage.getId(), List.of()).stream()
              .filter(application -> !scheduledApplicantIds.contains(application.getApplicantId()))
              .sorted(
                  Comparator.comparing(ApplicationEntity::getCreatedAt)
                      .thenComparing(ApplicationEntity::getId))
              .map(
                  application ->
                      new PendingInterviewApplicantRow(
                          application.getId(),
                          application.getApplicantId(),
                          fallbackName(
                              application.getName(), "지원자#" + application.getApplicantId()),
                          fallbackName(application.getEmail(), "")))
              .toList();

      result.add(
          new PendingInterviewStageRow(
              stage.getId(), stage.getStageName(), stage.getStageStep(), applicants));
    }
    return result;
  }

  // 접근 가능한 채용 공고 기준으로 이번 주 면접 일정을 조회용 row로 조합
  @Override
  public List<WeeklyInterviewScheduleRow> getWeeklySchedules(
      List<Long> recruitmentIds, LocalDateTime from, LocalDateTime to) {
    if (from == null || to == null || !from.isBefore(to)) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleEntity> schedules;
    if (recruitmentIds == null) {
      schedules =
          interviewScheduleRepository
              .findAllByTenantIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndInterviewStatusNotOrderByScheduledAtAsc(
                  tenantId, from, to, InterviewStatus.CANCELLED);
    } else {
      if (recruitmentIds.isEmpty()) {
        return List.of();
      }

      schedules =
          interviewScheduleRepository
              .findAllByTenantIdAndRecruitmentIdInAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndInterviewStatusNotOrderByScheduledAtAsc(
                  tenantId, recruitmentIds, from, to, InterviewStatus.CANCELLED);
    }

    if (schedules.isEmpty()) {
      return List.of();
    }

    List<Long> scheduleIds = schedules.stream().map(InterviewScheduleEntity::getId).toList();

    Map<Long, List<InterviewScheduleInterviewerEntity>> interviewersByScheduleId =
        interviewerRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleInterviewerEntity::getInterviewScheduleId));

    Map<Long, List<InterviewScheduleApplicantEntity>> applicantsByScheduleId =
        applicantMappingRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleApplicantEntity::getInterviewScheduleId));

    List<Long> interviewerIds =
        interviewersByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleInterviewerEntity::getUserId)
            .distinct()
            .toList();
    Map<Long, String> interviewerNameMap = resolveInterviewerNameMap(interviewerIds);

    List<Long> applicantIds =
        applicantsByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleApplicantEntity::getApplicantId)
            .distinct()
            .toList();
    Map<Long, String> applicantNameMap = resolveApplicantNameMap(tenantId, applicantIds);

    List<Long> scheduleRecruitmentIds =
        schedules.stream().map(InterviewScheduleEntity::getRecruitmentId).distinct().toList();
    Map<Long, String> recruitmentTitleMap = resolveRecruitmentTitleMap(scheduleRecruitmentIds);

    List<Long> recruitmentStageIds =
        schedules.stream()
            .map(InterviewScheduleEntity::getRecruitmentStageId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    Map<Long, String> stageNameMap = resolveStageNameMap(recruitmentStageIds);

    List<Long> meetingRoomIds =
        schedules.stream()
            .map(InterviewScheduleEntity::getMeetingRoomId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    Map<Long, String> locationMap = resolveMeetingRoomLocationMap(meetingRoomIds);

    List<WeeklyInterviewScheduleRow> result = new ArrayList<>();
    for (InterviewScheduleEntity schedule : schedules) {
      List<InterviewScheduleInterviewerEntity> interviewerMappings =
          interviewersByScheduleId.getOrDefault(schedule.getId(), List.of());
      List<InterviewScheduleApplicantEntity> applicantMappings =
          applicantsByScheduleId.getOrDefault(schedule.getId(), List.of());

      String recruitmentTitle = recruitmentTitleMap.get(schedule.getRecruitmentId());
      String stageName = stageNameMap.get(schedule.getRecruitmentStageId());
      if (!hasText(recruitmentTitle) || !hasText(stageName)) {
        continue;
      }

      Long firstInterviewerUserId =
          interviewerMappings.isEmpty() ? null : interviewerMappings.get(0).getUserId();

      String interviewerName =
          interviewerMappings.stream()
              .map(InterviewScheduleInterviewerEntity::getUserId)
              .map(id -> fallbackName(interviewerNameMap.get(id), "면접관#" + id))
              .distinct()
              .collect(Collectors.joining(", "));

      String applicantName =
          applicantMappings.stream()
              .map(InterviewScheduleApplicantEntity::getApplicantId)
              .map(id -> fallbackName(applicantNameMap.get(id), "지원자#" + id))
              .distinct()
              .collect(Collectors.joining(", "));

      result.add(
          new WeeklyInterviewScheduleRow(
              schedule.getId(),
              schedule.getRecruitmentId(),
              schedule.getScheduledAt(),
              schedule.getEndTime(),
              firstInterviewerUserId,
              interviewerName,
              buildWeeklyScheduleTitle(recruitmentTitle, stageName),
              applicantName,
              fallbackName(schedule.getMemo(), ""),
              fallbackName(locationMap.get(schedule.getMeetingRoomId()), "")));
    }
    return result;
  }

  @Override
  public Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds) {
    lockResourcesForScheduleCreation(meetingRoomId, interviewerIds, applicantIds);

    LocalDateTime end = scheduledAt.plusMinutes(durationMinutes);
    if (hasMeetingRoomConflict(meetingRoomId, scheduledAt, end)
        || hasParticipantConflict(interviewerIds, applicantIds, scheduledAt, end)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    InterviewScheduleEntity scheduleEntity =
        new InterviewScheduleEntity(
            recruitmentId,
            recruitmentStageId,
            meetingRoomId,
            scheduledAt,
            durationMinutes,
            memo == null ? "" : memo,
            InterviewStatus.CONFIRMED);
    InterviewScheduleEntity saved = interviewScheduleRepository.save(scheduleEntity);

    List<InterviewScheduleInterviewerEntity> interviewerEntities = new ArrayList<>();
    for (Long interviewerId : interviewerIds) {
      interviewerEntities.add(new InterviewScheduleInterviewerEntity(saved.getId(), interviewerId));
    }
    interviewerRepository.saveAll(interviewerEntities);

    List<InterviewScheduleApplicantEntity> applicantEntities = new ArrayList<>();
    for (Long applicantId : applicantIds) {
      applicantEntities.add(new InterviewScheduleApplicantEntity(saved.getId(), applicantId));
    }
    applicantMappingRepository.saveAll(applicantEntities);

    Long reservationUserId =
        userId != null ? userId : interviewerIds.stream().findFirst().orElse(0L);
    MeetingRoomReservationEntity reservation =
        MeetingRoomReservationEntity.builder()
            .meetingRoomId(meetingRoomId)
            .userId(reservationUserId)
            .interviewScheduleId(saved.getId())
            .startDatetime(scheduledAt)
            .endDatetime(end)
            .reservationStatus(MeetingRoomStatus.RESERVED)
            .build();
    MeetingRoomReservationEntity savedReservation =
        meetingRoomReservationRepository.save(reservation);

    createInterviewerSchedules(saved, savedReservation.getId(), interviewerIds);

    Map<String, Object> eventData = new HashMap<>();
    eventData.put("recruitmentId", recruitmentId);
    eventData.put("recruitmentStageId", recruitmentStageId);
    eventData.put("meetingRoomId", meetingRoomId);
    eventData.put("scheduledAt", scheduledAt.toString());
    eventData.put("durationMinutes", durationMinutes);
    eventData.put("memo", memo == null ? "" : memo);
    eventData.put("interviewerIds", interviewerIds);
    eventData.put("applicantIds", applicantIds);

    interviewScheduleHistoryRepository.save(
        InterviewScheduleHistoryEntity.builder()
            .interviewScheduleId(saved.getId())
            .eventType(InterviewEventType.CREATED)
            .eventData(eventData)
            .createdBy(reservationUserId)
            .build());

    return saved.getId();
  }

  @Override
  public void cancelConfirmedSchedule(Long userId, Long interviewScheduleId) {
    if (interviewScheduleId == null) {
      return;
    }

    String tenantId = requireTenantId();
    InterviewScheduleEntity schedule =
        interviewScheduleRepository
            .findByTenantIdAndIdAndInterviewStatusNot(
                tenantId, interviewScheduleId, InterviewStatus.CANCELLED)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    schedule.cancel();
    deleteInterviewerSchedules(schedule.getId());

    Map<String, Object> eventData = new HashMap<>();
    eventData.put("status", InterviewStatus.CANCELLED.name());
    eventData.put("reason", "MEETING_ROOM_RESERVATION_CANCELLED");

    interviewScheduleHistoryRepository.save(
        InterviewScheduleHistoryEntity.builder()
            .interviewScheduleId(schedule.getId())
            .eventType(InterviewEventType.STATUS_CHANGED)
            .eventData(eventData)
            .createdBy(userId != null ? userId : 0L)
            .build());
  }

  private void lockResourcesForScheduleCreation(
      Long meetingRoomId, List<Long> interviewerIds, List<Long> applicantIds) {
    String tenantId = requireTenantId();

    meetingRoomRepository
        .findByTenantIdAndId(tenantId, meetingRoomId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    List<Long> orderedInterviewerIds =
        interviewerIds == null
            ? List.of()
            : interviewerIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    if (!orderedInterviewerIds.isEmpty()) {
      if (memberReader.getMembersByUserIds(tenantId, orderedInterviewerIds).size()
          != orderedInterviewerIds.size()) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
      userRepository.findAllByIdInOrderByIdAsc(orderedInterviewerIds);
    }

    List<Long> orderedApplicantIds =
        applicantIds == null
            ? List.of()
            : applicantIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    if (!orderedApplicantIds.isEmpty()) {
      applicantRepository.findAllByTenantIdAndIdInOrderByIdAsc(tenantId, orderedApplicantIds);
    }
  }

  private boolean hasParticipantConflict(
      List<Long> interviewerIds, List<Long> applicantIds, LocalDateTime start, LocalDateTime end) {
    String tenantId = requireTenantId();
    boolean hasInterviewerIds = interviewerIds != null && !interviewerIds.isEmpty();
    boolean hasApplicantIds = applicantIds != null && !applicantIds.isEmpty();
    if (!hasInterviewerIds && !hasApplicantIds) {
      return false;
    }

    if (hasInterviewerIds) {
      List<Long> interviewerScheduleIds =
          interviewerRepository.findAllByTenantIdAndUserIdIn(tenantId, interviewerIds).stream()
              .map(InterviewScheduleInterviewerEntity::getInterviewScheduleId)
              .toList();
      if (hasScheduleOverlap(tenantId, interviewerScheduleIds, start, end)) {
        return true;
      }
    }

    if (hasApplicantIds) {
      List<Long> applicantScheduleIds =
          applicantMappingRepository
              .findAllByTenantIdAndApplicantIdIn(tenantId, applicantIds)
              .stream()
              .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
              .toList();
      return hasScheduleOverlap(tenantId, applicantScheduleIds, start, end);
    }

    return false;
  }

  // 같은 채용 단계에서 이미 일정이 잡힌 지원자 ID를 수집
  private Map<Long, Set<Long>> getScheduledApplicantIdsByStageId(
      String tenantId, Long recruitmentId, Set<Long> interviewStageIds) {
    if (interviewStageIds == null || interviewStageIds.isEmpty()) {
      return Map.of();
    }

    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndRecruitmentIdOrderByScheduledAtAsc(
            tenantId, recruitmentId);
    Map<Long, Long> stageIdByScheduleId = new HashMap<>();
    for (InterviewScheduleEntity schedule : schedules) {
      if (schedule.getInterviewStatus() == InterviewStatus.CANCELLED) {
        continue;
      }
      Long stageId = schedule.getRecruitmentStageId();
      if (stageId == null || !interviewStageIds.contains(stageId)) {
        continue;
      }
      stageIdByScheduleId.put(schedule.getId(), stageId);
    }

    if (stageIdByScheduleId.isEmpty()) {
      return Map.of();
    }

    Map<Long, Set<Long>> scheduledApplicantIdsByStageId = new HashMap<>();
    applicantMappingRepository
        .findAllByTenantIdAndInterviewScheduleIdIn(
            tenantId, new ArrayList<>(stageIdByScheduleId.keySet()))
        .forEach(
            applicantMapping -> {
              Long stageId = stageIdByScheduleId.get(applicantMapping.getInterviewScheduleId());
              if (stageId == null || applicantMapping.getApplicantId() == null) {
                return;
              }
              scheduledApplicantIdsByStageId
                  .computeIfAbsent(stageId, ignored -> new HashSet<>())
                  .add(applicantMapping.getApplicantId());
            });
    return scheduledApplicantIdsByStageId;
  }

  private boolean hasMeetingRoomConflict(
      Long meetingRoomId, LocalDateTime start, LocalDateTime end) {
    String tenantId = requireTenantId();
    long roomConflict =
        meetingRoomReservationRepository
            .countByTenantIdAndMeetingRoomIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId,
                meetingRoomId,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
                end,
                start);
    return roomConflict > 0;
  }

  private boolean hasScheduleOverlap(
      String tenantId, List<Long> scheduleIds, LocalDateTime start, LocalDateTime end) {
    if (scheduleIds.isEmpty()) {
      return false;
    }
    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, scheduleIds, InterviewStatus.CANCELLED, end);

    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime rowStart = schedule.getScheduledAt();
      LocalDateTime rowEnd = rowStart.plusMinutes(schedule.getDurationMinutes());
      if (rowStart.isBefore(end) && start.isBefore(rowEnd)) {
        return true;
      }
    }
    return false;
  }

  // 면접 확정 시 면접관 개인 캘린더에 읽기 전용 일정 행을 생성
  private void createInterviewerSchedules(
      InterviewScheduleEntity interviewSchedule, Long reservationId, List<Long> interviewerIds) {
    if (interviewSchedule == null || interviewerIds == null || interviewerIds.isEmpty()) {
      return;
    }

    String title =
        buildInterviewScheduleTitle(
            interviewSchedule.getRecruitmentId(), interviewSchedule.getRecruitmentStageId());

    List<ScheduleEntity> interviewerSchedules =
        interviewerIds.stream()
            .filter(userId -> userId != null && userId > 0)
            .distinct()
            .map(
                interviewerId ->
                    ScheduleEntity.builder()
                        .userId(interviewerId)
                        .title(title)
                        .description(fallbackName(interviewSchedule.getMemo(), ""))
                        .type(ScheduleType.INTERVIEW)
                        .startTime(interviewSchedule.getScheduledAt())
                        .endTime(interviewSchedule.getEndTime())
                        .isAllDay(false)
                        .roomId(interviewSchedule.getMeetingRoomId())
                        .reservationId(reservationId)
                        .isBusy(true)
                        .googleEventId(null)
                        .interviewScheduleId(interviewSchedule.getId())
                        .build())
            .toList();

    if (interviewerSchedules.isEmpty()) {
      return;
    }

    scheduleRepository.saveAll(interviewerSchedules);
  }

  // 면접 취소 시 연결된 면접관 일정 행을 함께 삭제
  private void deleteInterviewerSchedules(Long interviewScheduleId) {
    if (interviewScheduleId == null) {
      return;
    }

    scheduleRepository
        .findAllByInterviewScheduleIdAndStatus(interviewScheduleId, EntityStatus.ACTIVE)
        .forEach(
            schedule -> {
              schedule.updateGoogleEventId(null);
              schedule.deleted();
            });
  }

  // 채용 공고명과 단계명을 합쳐 면접 캘린더 제목을 생성
  private String buildInterviewScheduleTitle(Long recruitmentId, Long recruitmentStageId) {
    String recruitmentTitle =
        recruitmentId == null
            ? null
            : resolveRecruitmentTitleMap(List.of(recruitmentId)).get(recruitmentId);
    String stageName =
        recruitmentStageId == null
            ? null
            : resolveStageNameMap(List.of(recruitmentStageId)).get(recruitmentStageId);
    return buildWeeklyScheduleTitle(recruitmentTitle, stageName);
  }

  private Map<Long, String> resolveInterviewerNameMap(List<Long> interviewerIds) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    userRepository
        .findAllById(interviewerIds.stream().filter(id -> id != null && id > 0).distinct().toList())
        .forEach(
            user -> {
              if (user.getId() != null && hasText(user.getName())) {
                resolved.put(user.getId(), user.getName());
              }
            });

    return resolved;
  }

  private Map<Long, String> resolveApplicantNameMap(String tenantId, List<Long> applicantIds) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    applicantRepository
        .findByTenantIdAndIdIn(tenantId, applicantIds)
        .forEach(
            applicant -> {
              if (applicant.getId() != null && applicant.getName() != null) {
                resolved.put(applicant.getId(), applicant.getName());
              }
            });

    List<Long> unresolvedIds =
        applicantIds.stream()
            .filter(id -> id != null && !resolved.containsKey(id))
            .distinct()
            .toList();
    if (unresolvedIds.isEmpty()) {
      return resolved;
    }

    applicantRepository
        .findAllByIdIn(unresolvedIds)
        .forEach(
            applicant -> {
              if (applicant.getId() != null && hasText(applicant.getName())) {
                resolved.putIfAbsent(applicant.getId(), applicant.getName());
              }
            });
    return resolved;
  }

  // 면접 일정 제목 구성에 필요한 공고명을 조회
  private Map<Long, String> resolveRecruitmentTitleMap(List<Long> recruitmentIds) {
    if (recruitmentIds == null || recruitmentIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    recruitmentRepository
        .findAllById(recruitmentIds.stream().filter(id -> id != null && id > 0).distinct().toList())
        .forEach(
            recruitment -> {
              if (recruitment.isActive() && hasText(recruitment.getTitle())) {
                resolved.put(recruitment.getId(), recruitment.getTitle());
              }
            });
    return resolved;
  }

  // 면접 일정 제목 구성에 필요한 단계명을 조회
  private Map<Long, String> resolveStageNameMap(List<Long> recruitmentStageIds) {
    if (recruitmentStageIds == null || recruitmentStageIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    recruitmentStageRepository
        .findAllById(
            recruitmentStageIds.stream().filter(id -> id != null && id > 0).distinct().toList())
        .forEach(
            stage -> {
              if (stage.isActive() && hasText(stage.getStageName())) {
                resolved.put(stage.getId(), stage.getStageName());
              }
            });
    return resolved;
  }

  // 회의실 식별자를 화면 표시용 위치 문자열로 치환
  private Map<Long, String> resolveMeetingRoomLocationMap(List<Long> meetingRoomIds) {
    if (meetingRoomIds == null || meetingRoomIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    meetingRoomRepository
        .findAllById(meetingRoomIds.stream().filter(id -> id != null && id > 0).distinct().toList())
        .forEach(
            room -> {
              if (room.isActive()) {
                resolved.put(room.getId(), buildMeetingRoomLocation(room));
              }
            });
    return resolved;
  }

  // 공고명과 단계명을 합쳐 면접 일정 제목을 생성
  private String buildWeeklyScheduleTitle(String recruitmentTitle, String stageName) {
    if (hasText(recruitmentTitle) && hasText(stageName)) {
      return recruitmentTitle + " · " + stageName;
    }
    if (hasText(recruitmentTitle)) {
      return recruitmentTitle;
    }
    if (hasText(stageName)) {
      return stageName;
    }
    return "면접 일정";
  }

  // 회의실명과 층 정보를 합쳐 화면용 위치를 생성
  private String buildMeetingRoomLocation(MeetingRoomEntity room) {
    if (!hasText(room.getName())) {
      return "";
    }
    if (room.getLocation() == null) {
      return room.getName();
    }
    return room.getName() + " (" + room.getLocation() + "층)";
  }

  private String fallbackName(String value, String fallback) {
    return hasText(value) ? value : fallback;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
