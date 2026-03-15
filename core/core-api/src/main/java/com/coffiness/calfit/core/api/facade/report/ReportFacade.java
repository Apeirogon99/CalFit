package com.coffiness.calfit.core.api.facade.report;

import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportFacade {

  private final ApplicationRepository applicationRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final MeetingRoomRepository meetingRoomRepository;
  private final AutomationEventRepository automationEventRepository;
  private final AutomationRuleRepository automationRuleRepository;

  public KpiResult getKpi() {
    long totalApplicants = applicationRepository.countByStatus(EntityStatus.ACTIVE);

    List<Object[]> processIdCounts =
        applicationRepository.countGroupByRecruitmentProcessId(EntityStatus.ACTIVE);

    Map<Long, RecruitmentStageType> processIdToType = buildProcessIdToTypeMap();

    long passCount = 0;
    long failCount = 0;
    for (Object[] row : processIdCounts) {
      Long processId = (Long) row[0];
      Long count = (Long) row[1];
      RecruitmentStageType type = processIdToType.get(processId);
      if (type == RecruitmentStageType.PASS) passCount += count;
      else if (type == RecruitmentStageType.FAIL) failCount += count;
    }

    long inProgressCount = totalApplicants - passCount - failCount;
    long activeRecruitments =
        recruitmentRepository.countByRecruitmentStatusAndStatus(
            RecruitmentStatus.OPEN, EntityStatus.ACTIVE);
    long totalRecruitments = recruitmentRepository.countByStatus(EntityStatus.ACTIVE);
    double conversionRate =
        totalApplicants > 0 ? Math.round((double) passCount / totalApplicants * 1000) / 10.0 : 0.0;

    return new KpiResult(
        totalApplicants,
        inProgressCount,
        activeRecruitments,
        totalRecruitments,
        passCount,
        failCount,
        conversionRate);
  }

  public PipelineResult getPipeline(int months) {
    List<Object[]> processIdCounts =
        applicationRepository.countGroupByRecruitmentProcessId(EntityStatus.ACTIVE);

    Map<Long, RecruitmentStageType> processIdToType = buildProcessIdToTypeMap();

    Map<RecruitmentStageType, Long> typeCounts = new EnumMap<>(RecruitmentStageType.class);
    for (RecruitmentStageType type : RecruitmentStageType.values()) {
      typeCounts.put(type, 0L);
    }
    for (Object[] row : processIdCounts) {
      Long processId = (Long) row[0];
      Long count = (Long) row[1];
      RecruitmentStageType type = processIdToType.get(processId);
      if (type != null) {
        typeCounts.merge(type, count, Long::sum);
      }
    }

    List<StageDistributionItem> stageDistribution =
        Arrays.stream(RecruitmentStageType.values())
            .map(
                type ->
                    new StageDistributionItem(
                        type.name(), type.getDescription(), typeCounts.get(type)))
            .toList();

    LocalDateTime from =
        LocalDateTime.now()
            .minusMonths(months)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
    List<Object[]> monthCounts = applicationRepository.countGroupByMonth(EntityStatus.ACTIVE, from);

    Map<String, Long> monthCountMap = new LinkedHashMap<>();
    for (Object[] row : monthCounts) {
      int year = ((Number) row[0]).intValue();
      int month = ((Number) row[1]).intValue();
      String key = String.format("%d-%02d", year, month);
      monthCountMap.put(key, (Long) row[2]);
    }

    YearMonth now = YearMonth.now();
    List<MonthlyTrendItem> monthlyTrend = new ArrayList<>();
    for (int i = months - 1; i >= 0; i--) {
      YearMonth ym = now.minusMonths(i);
      String key = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
      monthlyTrend.add(new MonthlyTrendItem(key, monthCountMap.getOrDefault(key, 0L)));
    }

    return new PipelineResult(stageDistribution, monthlyTrend);
  }

  public List<RecruitmentPerformanceItem> getRecruitmentPerformance() {
    List<RecruitmentEntity> recruitments =
        recruitmentRepository.findByStatusOrderByCreatedAtDesc(EntityStatus.ACTIVE);

    if (recruitments.isEmpty()) return List.of();

    List<Object[]> recruitmentIdCounts =
        applicationRepository.countGroupByRecruitmentId(EntityStatus.ACTIVE);
    Map<Long, Long> applicantCountMap =
        recruitmentIdCounts.stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

    List<Object[]> processIdCounts =
        applicationRepository.countGroupByRecruitmentProcessId(EntityStatus.ACTIVE);
    Map<Long, RecruitmentStageType> processIdToType = buildProcessIdToTypeMap();

    List<Long> recruitmentIds = recruitments.stream().map(RecruitmentEntity::getId).toList();
    List<RecruitmentStageEntity> stages =
        recruitmentStageRepository.findByRecruitmentIdIn(recruitmentIds);
    Map<Long, Long> passStageToRecruitment =
        stages.stream()
            .filter(s -> s.getStageType() == RecruitmentStageType.PASS)
            .collect(
                Collectors.toMap(
                    RecruitmentStageEntity::getId, RecruitmentStageEntity::getRecruitmentId));

    Map<Long, Long> passCountByRecruitment = new HashMap<>();
    for (Object[] row : processIdCounts) {
      Long processId = (Long) row[0];
      Long count = (Long) row[1];
      if (processIdToType.get(processId) == RecruitmentStageType.PASS) {
        Long recruitmentId = passStageToRecruitment.get(processId);
        if (recruitmentId != null) {
          passCountByRecruitment.merge(recruitmentId, count, Long::sum);
        }
      }
    }

    return recruitments.stream()
        .map(
            r ->
                new RecruitmentPerformanceItem(
                    r.getId(),
                    r.getTitle(),
                    r.getRecruitmentStatus().name(),
                    r.getTargetCount(),
                    applicantCountMap.getOrDefault(r.getId(), 0L),
                    passCountByRecruitment.getOrDefault(r.getId(), 0L)))
        .toList();
  }

  private Map<Long, RecruitmentStageType> buildProcessIdToTypeMap() {
    return recruitmentStageRepository.findAll().stream()
        .collect(
            Collectors.toMap(
                RecruitmentStageEntity::getId, RecruitmentStageEntity::getStageType, (a, b) -> a));
  }

  public InterviewReportResult getInterviewReport() {
    List<Object[]> statusCounts = interviewScheduleRepository.countGroupByInterviewStatus();

    Map<InterviewStatus, Long> statusMap = new EnumMap<>(InterviewStatus.class);
    for (InterviewStatus s : InterviewStatus.values()) {
      statusMap.put(s, 0L);
    }
    for (Object[] row : statusCounts) {
      InterviewStatus status = (InterviewStatus) row[0];
      Long count = (Long) row[1];
      statusMap.put(status, count);
    }

    long totalInterviews = statusMap.values().stream().mapToLong(Long::longValue).sum();
    long confirmedCount = statusMap.get(InterviewStatus.CONFIRMED);
    long completedCount = statusMap.get(InterviewStatus.COMPLETED);
    long cancelledCount = statusMap.get(InterviewStatus.CANCELLED);
    long pendingCount = statusMap.get(InterviewStatus.PENDING);
    double cancelRate =
        totalInterviews > 0
            ? Math.round((double) cancelledCount / totalInterviews * 1000) / 10.0
            : 0.0;

    Map<InterviewStatus, String> interviewStatusLabels =
        Map.of(
            InterviewStatus.PENDING, "대기",
            InterviewStatus.CONFIRMED, "확정",
            InterviewStatus.CANCELLED, "취소",
            InterviewStatus.COMPLETED, "완료");
    List<StatusCountItem> statusDistribution =
        Arrays.stream(InterviewStatus.values())
            .map(
                s ->
                    new StatusCountItem(
                        s.name(),
                        interviewStatusLabels.getOrDefault(s, s.name()),
                        statusMap.get(s)))
            .toList();

    Double avgDuration =
        interviewScheduleRepository.averageDurationMinutes(
            List.of(InterviewStatus.CONFIRMED, InterviewStatus.COMPLETED));
    double avgDurationMinutes = avgDuration != null ? Math.round(avgDuration * 10) / 10.0 : 0.0;

    List<Object[]> roomCounts = interviewScheduleRepository.countGroupByMeetingRoomId();
    Map<Long, String> roomNameMap = buildRoomNameMap();
    List<RoomUsageItem> roomUsage =
        roomCounts.stream()
            .map(
                row -> {
                  Long roomId = (Long) row[0];
                  Long count = (Long) row[1];
                  String name = roomNameMap.getOrDefault(roomId, "삭제된 회의실");
                  return new RoomUsageItem(name, count);
                })
            .sorted((a, b) -> Long.compare(b.count(), a.count()))
            .toList();

    return new InterviewReportResult(
        totalInterviews,
        confirmedCount,
        completedCount,
        cancelledCount,
        pendingCount,
        cancelRate,
        avgDurationMinutes,
        statusDistribution,
        roomUsage);
  }

  public AutomationReportResult getAutomationReport() {
    List<Object[]> eventStatusCounts = automationEventRepository.countGroupByEventStatus();
    Map<AutomationEventStatus, Long> eventStatusMap = new EnumMap<>(AutomationEventStatus.class);
    for (AutomationEventStatus s : AutomationEventStatus.values()) {
      eventStatusMap.put(s, 0L);
    }
    for (Object[] row : eventStatusCounts) {
      AutomationEventStatus status = (AutomationEventStatus) row[0];
      Long count = (Long) row[1];
      eventStatusMap.put(status, count);
    }

    long totalEvents = eventStatusMap.values().stream().mapToLong(Long::longValue).sum();
    long successCount = eventStatusMap.get(AutomationEventStatus.SUCCESS);
    long failedCount = eventStatusMap.get(AutomationEventStatus.FAILED);
    long eventPendingCount = eventStatusMap.get(AutomationEventStatus.PENDING);
    double failureRate =
        totalEvents > 0 ? Math.round((double) failedCount / totalEvents * 1000) / 10.0 : 0.0;

    List<StatusCountItem> eventStatusDistribution =
        Arrays.stream(AutomationEventStatus.values())
            .map(s -> new StatusCountItem(s.name(), s.name(), eventStatusMap.get(s)))
            .toList();

    List<Object[]> actionTypeCounts = automationEventRepository.countGroupByActionType();
    Map<AutomationActionType, Long> actionTypeMap = new EnumMap<>(AutomationActionType.class);
    for (AutomationActionType t : AutomationActionType.values()) {
      actionTypeMap.put(t, 0L);
    }
    for (Object[] row : actionTypeCounts) {
      AutomationActionType type = (AutomationActionType) row[0];
      Long count = (Long) row[1];
      actionTypeMap.put(type, count);
    }

    List<ActionTypeCountItem> actionTypeDistribution =
        Arrays.stream(AutomationActionType.values())
            .map(t -> new ActionTypeCountItem(t.name(), actionTypeMap.get(t)))
            .toList();

    long totalRules = automationRuleRepository.countByStatus(EntityStatus.ACTIVE);
    long automatedRecruitments =
        automationRuleRepository.countDistinctRecruitmentIdByStatus(EntityStatus.ACTIVE);
    long totalRecruitments = recruitmentRepository.countByStatus(EntityStatus.ACTIVE);
    double adoptionRate =
        totalRecruitments > 0
            ? Math.round((double) automatedRecruitments / totalRecruitments * 1000) / 10.0
            : 0.0;

    return new AutomationReportResult(
        totalRules,
        totalEvents,
        successCount,
        failedCount,
        eventPendingCount,
        failureRate,
        eventStatusDistribution,
        actionTypeDistribution,
        automatedRecruitments,
        totalRecruitments,
        adoptionRate);
  }

  private Map<Long, String> buildRoomNameMap() {
    return meetingRoomRepository.findAllByStatusOrderByNameAsc(EntityStatus.ACTIVE).stream()
        .collect(Collectors.toMap(MeetingRoomEntity::getId, MeetingRoomEntity::getName));
  }

  public record KpiResult(
      long totalApplicants,
      long inProgressCount,
      long activeRecruitments,
      long totalRecruitments,
      long passCount,
      long failCount,
      double conversionRate) {}

  public record StageDistributionItem(String stageType, String label, long count) {}

  public record MonthlyTrendItem(String month, long count) {}

  public record PipelineResult(
      List<StageDistributionItem> stageDistribution, List<MonthlyTrendItem> monthlyTrend) {}

  public record RecruitmentPerformanceItem(
      Long recruitmentId,
      String title,
      String status,
      int targetCount,
      long applicantCount,
      long passCount) {}

  // ── Phase 2 Records ──

  public record StatusCountItem(String status, String label, long count) {}

  public record RoomUsageItem(String roomName, long count) {}

  public record InterviewReportResult(
      long totalInterviews,
      long confirmedCount,
      long completedCount,
      long cancelledCount,
      long pendingCount,
      double cancelRate,
      double avgDurationMinutes,
      List<StatusCountItem> statusDistribution,
      List<RoomUsageItem> roomUsage) {}

  public record ActionTypeCountItem(String actionType, long count) {}

  public record AutomationReportResult(
      long totalRules,
      long totalEvents,
      long successCount,
      long failedCount,
      long pendingCount,
      double failureRate,
      List<StatusCountItem> eventStatusDistribution,
      List<ActionTypeCountItem> actionTypeDistribution,
      long automatedRecruitments,
      long totalRecruitments,
      double adoptionRate) {}
}
