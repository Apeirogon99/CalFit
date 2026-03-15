package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

  private final RecruitmentStore recruitmentStore;
  private final RecruitmentHistoryAppender recruitmentHistoryAppender;
  private final RecruitmentReader recruitmentReader;
  private final MemberReader memberReader;

  public Long createRecruitment(long userId, RecruitmentCreateRequest request) {

    validateCreatableSchedule(request.startDate(), request.endDate(), LocalDateTime.now());

    List<RecruitmentStage> stages = toStagesWithRequiredFail(request.stages());

    RecruitmentStatus initialStatus =
        resolveStatusAtCreation(request.startDate(), request.endDate(), LocalDateTime.now());

    Recruitment newRecruitment =
        new Recruitment(
            null,
            userId,
            request.title(),
            request.contents(),
            initialStatus,
            request.targetCount(),
            request.startDate(),
            request.endDate(),
            request.applicationTemplateId(),
            request.careerType(),
            request.minExperienceYears(),
            request.maxExperienceYears(),
            request.leadGroupId(),
            request.referenceGroupIds(),
            request.interviewerIds(),
            stages);

    Recruitment saveRecruitment = recruitmentStore.store(newRecruitment);

    recruitmentHistoryAppender.append(
        saveRecruitment.id(),
        saveRecruitment.creatorId(),
        RecruitmentActionType.RECRUITMENT_CREATED,
        "공고 생성",
        saveRecruitment);

    return saveRecruitment.id();
  }

  @Transactional(readOnly = true)
  public List<RecruitmentListInfo> getRecruitmentList(
      long userId, RecruitmentStatus recruitmentStatus, Pageable pageable) {
    Member member = getRequiredMember(userId);
    return recruitmentReader.readList(
        userId,
        member.groupId(),
        member.memberType() == MemberType.HR,
        recruitmentStatus,
        pageable);
  }

  @Transactional(readOnly = true)
  public RecruitmentDetailInfo getRecruitmentDetail(long userId, Long recruitmentId) {
    return recruitmentReader.readDetail(recruitmentId);
  }

  public Recruitment updateRecruitment(
      long memberId, Long recruitmentId, RecruitmentUpdateRequest request) {

    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    recruitment.validateUpdatable(LocalDateTime.now());

    List<RecruitmentStage> newStages = toStagesWithRequiredFail(request.stages());

    Recruitment updatedRecruitment =
        recruitment.updateDetails(
            request.title(),
            request.contents(),
            request.targetCount(),
            request.startDate(),
            request.endDate(),
            request.applicationTemplateId(),
            request.careerType(),
            request.minExperienceYears(),
            request.maxExperienceYears(),
            request.leadGroupId(),
            request.referenceGroupIds(),
            request.interviewerIds(),
            newStages);

    Recruitment savedRecruitment = recruitmentStore.update(updatedRecruitment);

    recruitmentHistoryAppender.append(
        updatedRecruitment.id(),
        memberId,
        RecruitmentActionType.RECRUITMENT_INFO_UPDATED,
        "채용 공고 수정",
        updatedRecruitment);

    return savedRecruitment;
  }

  // 게시 여부와 무관하게 채용 공고의 면접관 목록만 별도로 수정
  public Recruitment updateRecruitmentInterviewers(
      long memberId, Long recruitmentId, List<Long> interviewerIds) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    Recruitment updatedRecruitment = recruitment.updateInterviewers(interviewerIds);
    Recruitment savedRecruitment = recruitmentStore.updateInterviewers(updatedRecruitment);

    appendInterviewerHistory(memberId, recruitment, updatedRecruitment);

    return savedRecruitment;
  }

  // DRAFT 공고를 즉시 게시하고 실제 시작 시각도 현재로 반영
  public Recruitment publishRecruitmentNow(long memberId, Long recruitmentId) {
    return publishRecruitmentNow(memberId, recruitmentId, LocalDateTime.now());
  }

  // 테스트와 스케줄링 검증을 위해 기준 시각을 주입받는 게시 전환 로직
  public Recruitment publishRecruitmentNow(
      long memberId, Long recruitmentId, LocalDateTime currentTime) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    Recruitment publishedRecruitment = recruitment.publishNow(currentTime);
    Recruitment savedRecruitment = recruitmentStore.update(publishedRecruitment);

    recruitmentHistoryAppender.append(
        savedRecruitment.id(),
        memberId,
        RecruitmentActionType.RECRUITMENT_OPENED,
        "채용 공고 즉시 게시",
        savedRecruitment);

    return savedRecruitment;
  }

  public void assertCanAccess(long memberId, Long recruitmentId) {
    RecruitmentDetailInfo recruitment = recruitmentReader.readDetail(recruitmentId);

    boolean isInterviewer =
        recruitment.interviewers().stream()
            .anyMatch(interviewer -> interviewer.userId().equals(memberId));

    if (isInterviewer) {
      return;
    }

    throw new IllegalArgumentException("해당 채용 공고에 접근할 권한이 없습니다.");
  }

  public void deleteRecruitment(Long memberId, Long recruitmentId) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    if (recruitment.recruitmentStatus() != RecruitmentStatus.DRAFT) {
      throw new IllegalArgumentException("진행중이거나 마감된 채용 공고는 삭제할 수 없습니다.");
    }

    recruitmentStore.delete(recruitmentId);

    recruitmentHistoryAppender.append(
        recruitmentId, memberId, RecruitmentActionType.RECRUITMENT_DELETE, "채용 공고 삭제", recruitment);
  }

  public RecruitmentStatusTransitionResult updateRecruitmentStatusBySchedule(
      LocalDateTime currentTime) {
    int closedCount = recruitmentStore.closeEndedRecruitments(currentTime);
    int openedCount = recruitmentStore.openScheduledRecruitments(currentTime);
    return new RecruitmentStatusTransitionResult(openedCount, closedCount);
  }

  public RecruitmentStatusTransitionResult updateRecruitmentStatusBySchedule() {
    return updateRecruitmentStatusBySchedule(LocalDateTime.now());
  }

  // 면접관 추가/해제 이력을 변경 내용에 맞춰 남김
  private void appendInterviewerHistory(
      long memberId, Recruitment beforeRecruitment, Recruitment afterRecruitment) {
    Set<Long> beforeIds = new LinkedHashSet<>(beforeRecruitment.interviewerIds());
    Set<Long> afterIds = new LinkedHashSet<>(afterRecruitment.interviewerIds());

    Set<Long> addedIds = new LinkedHashSet<>(afterIds);
    addedIds.removeAll(beforeIds);
    if (!addedIds.isEmpty()) {
      recruitmentHistoryAppender.append(
          afterRecruitment.id(),
          memberId,
          RecruitmentActionType.INTERVIEWER_ADDED,
          "면접관 설정 수정",
          afterRecruitment);
    }

    Set<Long> removedIds = new LinkedHashSet<>(beforeIds);
    removedIds.removeAll(afterIds);
    if (!removedIds.isEmpty()) {
      recruitmentHistoryAppender.append(
          afterRecruitment.id(),
          memberId,
          RecruitmentActionType.INTERVIEWER_REMOVED,
          "면접관 설정 수정",
          afterRecruitment);
    }
  }

  // 현재 워크스페이스 기준으로 목록 조회에 사용할 멤버 정보를 가져옴
  private Member getRequiredMember(long userId) {
    String workspaceId = TenantContext.getTenantId();
    return memberReader.getMember(workspaceId, userId);
  }

  private RecruitmentStatus resolveStatusAtCreation(
      LocalDateTime startDate, LocalDateTime endDate, LocalDateTime currentTime) {
    if (endDate != null && !endDate.isAfter(currentTime)) {
      return RecruitmentStatus.CLOSED;
    }
    if (startDate != null && !startDate.isAfter(currentTime)) {
      return RecruitmentStatus.OPEN;
    }
    return RecruitmentStatus.DRAFT;
  }

  private void validateCreatableSchedule(
      LocalDateTime startDate, LocalDateTime endDate, LocalDateTime currentTime) {
    if (endDate != null && !endDate.isAfter(currentTime)) {
      throw new IllegalArgumentException("이미 종료된 기간으로는 채용 공고를 생성할 수 없습니다.");
    }
  }

  private List<RecruitmentStage> toStagesWithRequiredFail(
      List<RecruitmentStageRequest> stageRequests) {
    List<RecruitmentStage> stages =
        stageRequests == null
            ? List.of()
            : stageRequests.stream()
                .map(s -> new RecruitmentStage(null, s.stageName(), s.stageStep(), s.stageType()))
                .toList();

    boolean hasFailStage =
        stages.stream().anyMatch(stage -> stage.stageType() == RecruitmentStageType.FAIL);
    if (hasFailStage) {
      return stages;
    }

    int nextStep = stages.stream().mapToInt(RecruitmentStage::stageStep).max().orElse(0) + 1;
    return Stream.concat(
            stages.stream(),
            Stream.of(new RecruitmentStage(null, "불합격", nextStep, RecruitmentStageType.FAIL)))
        .toList();
  }
}
