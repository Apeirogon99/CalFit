package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import com.coffiness.calfit.domain.recruitment.*;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.group.GroupReader;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.recruitment.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentReaderImpl implements RecruitmentReader {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;
  private final ApplicationRepository applicationRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;

  private final UserReader userReader;
  private final GroupReader groupReader;

  @Override
  public List<RecruitmentListInfo> readList(
      Long userId,
      Long groupId,
      boolean hrMember,
      RecruitmentStatus recruitmentStatus,
      Pageable pageable) {
    Page<RecruitmentEntity> recruitments =
        hrMember
            ? readAllRecruitments(recruitmentStatus, pageable)
            : recruitmentRepository.findAccessibleByUserIdAndGroupIdAndStatus(
                userId, groupId, recruitmentStatus, EntityStatus.ACTIVE, pageable);

    if (recruitments.isEmpty()) {
      return List.of();
    }

    List<Long> recruitmentIds = recruitments.stream().map(RecruitmentEntity::getId).toList();
    String tenantId = TenantContext.getTenantId();

    // 단계
    Map<Long, List<RecruitmentStageEntity>> stageMap =
        recruitmentStageRepository.findByRecruitmentIdIn(recruitmentIds).stream()
            .collect(Collectors.groupingBy(RecruitmentStageEntity::getRecruitmentId));

    // 공고별 참조 조직
    Map<Long, List<Long>> referenceGroupMap =
        recruitmentReferenceGroupRepository.findByRecruitmentIdIn(recruitmentIds).stream()
            .filter(referenceGroup -> referenceGroup.getGroupId() != null)
            .collect(
                Collectors.groupingBy(
                    RecruitmentReferenceGroupEntity::getRecruitmentId,
                    Collectors.mapping(
                        RecruitmentReferenceGroupEntity::getGroupId, Collectors.toList())));

    // 단계별 지원자 수
    Map<Long, Map<Long, Integer>> applicantCountMapByRecruitment =
        applicationRepository
            .countGroupByRecruitmentIdAndRecruitmentProcessId(
                tenantId, recruitmentIds, EntityStatus.ACTIVE)
            .stream()
            .collect(
                Collectors.groupingBy(
                    row -> ((Number) row[0]).longValue(),
                    Collectors.toMap(
                        row -> ((Number) row[1]).longValue(),
                        row -> ((Number) row[2]).intValue())));

    // 면접관
    List<RecruitmentInterviewerEntity> allInterviewers =
        recruitmentInterviewerRepository.findByRecruitmentIdIn(recruitmentIds);
    Map<Long, List<RecruitmentInterviewerEntity>> interviewerMap =
        allInterviewers.stream()
            .collect(Collectors.groupingBy(RecruitmentInterviewerEntity::getRecruitmentId));

    // 외부 유저 도메인 정보 가져오기
    List<Long> userIds =
        allInterviewers.stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, String> userNameMap =
        userReader.getUsers(userIds).stream()
            .collect(Collectors.toMap(UserInfo::id, UserInfo::name));

    // 그룹 도메인 정보 외부에서 가져오기
    List<Long> leadGroupIds =
        recruitments.stream()
            .map(RecruitmentEntity::getLeadGroupId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, String> groupNameMap =
        leadGroupIds.isEmpty() ? Map.of() : groupReader.getGroupNameMap(leadGroupIds);

    return recruitments.stream()
        .map(
            entity -> {
              Long recruitmentId = entity.getId();

              String groupName =
                  entity.getLeadGroupId() != null
                      ? groupNameMap.getOrDefault(entity.getLeadGroupId(), "부서 미지정")
                      : "부서 미지정";

              List<RecruitmentListInfo.StageSummaryInfo> stageInfos =
                  buildStageSummaries(
                      stageMap.getOrDefault(recruitmentId, List.of()),
                      applicantCountMapByRecruitment.getOrDefault(recruitmentId, Map.of()));

              List<RecruitmentListInfo.AssigneeSummaryInfo> assigneeInfos =
                  interviewerMap.getOrDefault(recruitmentId, List.of()).stream()
                      .map(
                          i -> {
                            Long interviewerUserId = i.getUserId();
                            String name = userNameMap.getOrDefault(interviewerUserId, "알 수 없는 사용자");
                            return new RecruitmentListInfo.AssigneeSummaryInfo(
                                interviewerUserId, name);
                          })
                      .toList();

              return new RecruitmentListInfo(
                  recruitmentId,
                  entity.getTitle(),
                  groupName,
                  entity.getLeadGroupId(),
                  referenceGroupMap.getOrDefault(recruitmentId, List.of()),
                  entity.getCareerType(),
                  entity.getMinExperienceYears(),
                  entity.getMaxExperienceYears(),
                  entity.getStartDate(),
                  entity.getEndDate(),
                  entity.getRecruitmentStatus(),
                  stageInfos,
                  assigneeInfos);
            })
        .toList();
  }

  @Override
  public List<Long> readAccessibleRecruitmentIds(Long userId, Long groupId) {
    return recruitmentRepository.findAccessibleIdsByUserIdAndGroupIdAndStatus(
        userId, groupId, EntityStatus.ACTIVE);
  }

  // HR 목록 조회는 기존처럼 워크스페이스 전체 채용 공고를 반환한다.
  private Page<RecruitmentEntity> readAllRecruitments(
      RecruitmentStatus recruitmentStatus, Pageable pageable) {
    if (recruitmentStatus == null) {
      return recruitmentRepository.findByStatus(EntityStatus.ACTIVE, pageable);
    }
    return recruitmentRepository.findByRecruitmentStatusAndStatus(
        recruitmentStatus, EntityStatus.ACTIVE, pageable);
  }

  // 목록 화면용 단계 정보를 만들고 최종 합격 단계를 항상 마지막에 보정
  private List<RecruitmentListInfo.StageSummaryInfo> buildStageSummaries(
      List<RecruitmentStageEntity> stages, Map<Long, Integer> applicantCountMap) {
    List<RecruitmentStageEntity> visibleStages =
        stages.stream()
            .filter(stage -> stage.getStageType() != RecruitmentStageType.FAIL)
            .sorted(Comparator.comparing(RecruitmentStageEntity::getStageStep))
            .toList();

    List<RecruitmentListInfo.StageSummaryInfo> stageInfos = new ArrayList<>();
    RecruitmentStageEntity passStage = null;

    for (RecruitmentStageEntity stage : visibleStages) {
      int applicantCount = applicantCountMap.getOrDefault(stage.getId(), 0);
      if (stage.getStageType() == RecruitmentStageType.PASS) {
        if (passStage == null) {
          passStage = stage;
        }
        continue;
      }
      stageInfos.add(
          new RecruitmentListInfo.StageSummaryInfo(
              stage.getId(), stage.getStageName(), applicantCount));
    }

    stageInfos.add(buildPassStageSummary(passStage, applicantCountMap));
    return stageInfos;
  }

  // 최종 합격 단계가 없으면 목록 마지막에 기본 단계를 추가
  private RecruitmentListInfo.StageSummaryInfo buildPassStageSummary(
      RecruitmentStageEntity passStage, Map<Long, Integer> applicantCountMap) {
    if (passStage == null) {
      return new RecruitmentListInfo.StageSummaryInfo(null, "최종 합격", 0);
    }

    int applicantCount = applicantCountMap.getOrDefault(passStage.getId(), 0);
    return new RecruitmentListInfo.StageSummaryInfo(
        passStage.getId(), passStage.getStageName(), applicantCount);
  }

  @Override
  public RecruitmentDetailInfo readDetail(Long recruitmentId) {
    // TODO: 에러 처리 (CoreException) 추후 삽입
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));
    String tenantId = TenantContext.getTenantId();

    // Group Name 가져오기
    String groupName = "부서 미지정";
    if (entity.getLeadGroupId() != null) {
      Map<Long, String> groupNameMap =
          groupReader.getGroupNameMap(List.of(entity.getLeadGroupId()));
      groupName = groupNameMap.getOrDefault(entity.getLeadGroupId(), "부서 미지정");
    }

    // 면접관 정보 매핑
    List<RecruitmentInterviewerEntity> interviewers =
        recruitmentInterviewerRepository.findByRecruitmentId(recruitmentId);
    List<Long> userIds =
        interviewers.stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, String> userNameMap =
        userReader.getUsers(userIds).stream()
            .collect(Collectors.toMap(UserInfo::id, UserInfo::name));

    List<InterviewerInfo> interviewerInfos =
        interviewers.stream()
            .filter(i -> i.getUserId() != null)
            .map(
                i -> {
                  Long userId = i.getUserId();
                  String name = userNameMap.getOrDefault(userId, "알 수 없는 사용자");
                  // 체크박스는 우선 true
                  return new InterviewerInfo(userId, name, true);
                })
            .toList();

    List<Long> referenceGroupIds =
        recruitmentReferenceGroupRepository.findByRecruitmentId(recruitmentId).stream()
            .map(RecruitmentReferenceGroupEntity::getGroupId)
            .filter(Objects::nonNull)
            .toList();

    List<RecruitmentStage> stages =
        recruitmentStageRepository.findByRecruitmentIdOrderByStageStepAsc(recruitmentId).stream()
            .map(
                stage ->
                    new RecruitmentStage(
                        stage.getId(),
                        stage.getStageName(),
                        stage.getStageStep(),
                        stage.getStageType()))
            .toList();

    // D-Day 및 링크 URL
    int dDay = (int) ChronoUnit.DAYS.between(LocalDate.now(), entity.getEndDate().toLocalDate());
    // TODO : 공고 링크 연결
    String shareUrl = "https://careers.nexus.ai/jobs/" + recruitmentId;
    int totalApplicants = countTotalApplicants(tenantId, recruitmentId);
    int processingInterviewCount = countProcessingInterviewCount(tenantId, recruitmentId);

    return new RecruitmentDetailInfo(
        entity.getId(),
        entity.getTitle(),
        entity.getContents(),
        groupName,
        entity.getLeadGroupId(),
        referenceGroupIds,
        entity.getTargetCount(),
        entity.getApplicationTemplateId(),
        entity.getCareerType(),
        entity.getMinExperienceYears(),
        entity.getMaxExperienceYears(),
        entity.getStartDate(),
        entity.getEndDate(),
        totalApplicants,
        processingInterviewCount,
        dDay,
        shareUrl,
        entity.getRecruitmentStatus(),
        interviewerInfos,
        stages);
  }

  @Override
  public Recruitment readById(Long recruitmentId) {
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    return toRecruitment(entity);
  }

  @Override
  public List<Recruitment> readScheduledToOpen(LocalDateTime currentTime) {
    return recruitmentRepository
        .findScheduledToOpen(currentTime, EntityStatus.ACTIVE, RecruitmentStatus.DRAFT)
        .stream()
        .map(this::toRecruitment)
        .toList();
  }

  @Override
  public List<Recruitment> readScheduledToClose(LocalDateTime currentTime) {
    return recruitmentRepository
        .findScheduledToClose(
            currentTime,
            EntityStatus.ACTIVE,
            List.of(RecruitmentStatus.DRAFT, RecruitmentStatus.OPEN))
        .stream()
        .map(this::toRecruitment)
        .toList();
  }

  // 채용 공고 엔티티 도메인 변환
  private Recruitment toRecruitment(RecruitmentEntity entity) {
    Long recruitmentId = entity.getId();

    List<RecruitmentStage> stages =
        recruitmentStageRepository.findByRecruitmentId(recruitmentId).stream()
            .map(
                s ->
                    new RecruitmentStage(
                        s.getId(), s.getStageName(), s.getStageStep(), s.getStageType()))
            .toList();

    List<Long> interviewerIds =
        recruitmentInterviewerRepository.findByRecruitmentId(recruitmentId).stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .toList();

    List<Long> referenceGroupIds =
        recruitmentReferenceGroupRepository.findByRecruitmentId(recruitmentId).stream()
            .map(RecruitmentReferenceGroupEntity::getGroupId)
            .filter(Objects::nonNull)
            .toList();

    return new Recruitment(
        recruitmentId,
        entity.getCreatorId(),
        entity.getTitle(),
        entity.getContents(),
        entity.getRecruitmentStatus(),
        entity.getTargetCount(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getApplicationTemplateId(),
        entity.getCareerType(),
        entity.getMinExperienceYears(),
        entity.getMaxExperienceYears(),
        entity.getLeadGroupId(),
        referenceGroupIds,
        interviewerIds,
        stages);
  }

  @Override
  public Map<String, Long> countOpenRecruitmentsByTenant() {
    List<Object[]> counts = recruitmentRepository.countOpenRecruitmentsByTenant();
    return counts.stream()
        .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
  }

  @Override
  public List<OpenRecruitmentInfo> readOpenByTenantId(String tenantId) {
    return recruitmentRepository.findOpenRecruitmentsByTenantId(tenantId).stream()
        .map(
            e ->
                new OpenRecruitmentInfo(
                    e.getId(),
                    e.getTitle(),
                    e.getContents(),
                    e.getCareerType(),
                    e.getTargetCount(),
                    e.getMinExperienceYears(),
                    e.getMaxExperienceYears(),
                    e.getStartDate(),
                    e.getEndDate()))
        .toList();
  }

  @Override
  public List<OpenRecruitmentInfo> readOpenByTenantIdAndSearch(String tenantId, String search) {
    return recruitmentRepository.findOpenRecruitmentsByTenantIdAndSearch(tenantId, search).stream()
        .map(
            e ->
                new OpenRecruitmentInfo(
                    e.getId(),
                    e.getTitle(),
                    e.getContents(),
                    e.getCareerType(),
                    e.getTargetCount(),
                    e.getMinExperienceYears(),
                    e.getMaxExperienceYears(),
                    e.getStartDate(),
                    e.getEndDate()))
        .toList();
  }

  // 채용 공고의 활성 지원자 수를 집계
  private int countTotalApplicants(String tenantId, Long recruitmentId) {
    return (int)
        applicationRepository.countByTenantIdAndRecruitmentIdAndStatus(
            tenantId, recruitmentId, EntityStatus.ACTIVE);
  }

  // 채용 공고의 진행 중 면접 수를 집계
  private int countProcessingInterviewCount(String tenantId, Long recruitmentId) {
    LocalDateTime now = LocalDateTime.now();
    return (int)
        interviewScheduleRepository
            .findAllByTenantIdAndRecruitmentIdOrderByScheduledAtAsc(tenantId, recruitmentId)
            .stream()
            .filter(schedule -> isProcessingInterview(schedule, now))
            .count();
  }

  // 취소되었거나 종료된 면접은 진행 중 집계에서 제외
  private boolean isProcessingInterview(InterviewScheduleEntity schedule, LocalDateTime now) {
    return schedule.getInterviewStatus() != InterviewStatus.CANCELLED
        && schedule.getInterviewStatus() != InterviewStatus.COMPLETED
        && schedule.getEndTime().isAfter(now);
  }
}
