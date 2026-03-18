package com.coffiness.calfit.core.api.facade.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentInterviewersUpdateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.interview.WeeklyInterviewScheduleItem;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentReader;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecruitmentFacade {

  private final InterviewReader interviewReader;
  private final MemberReader memberReader;
  private final RecruitmentService recruitmentService;
  private final RecruitmentReader recruitmentReader;
  private final ApplicationTemplateRepository applicationTemplateRepository;

  // 채용 공고 생성 전에 요청 사용자의 HR 권한을 확인
  @Transactional
  public Long createRecruitment(long userId, RecruitmentCreateRequest request) {
    Member member = getRequiredMember(userId);
    validateHrMember(member, "채용 담당자만 채용 공고를 생성할 수 있습니다.");
    validateTemplateInUse(request.applicationTemplateId());
    return recruitmentService.createRecruitment(userId, request);
  }

  // 채용 공고 목록 조회
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

  // 채용 공고 상세 조회
  @Transactional(readOnly = true)
  public RecruitmentDetailInfo getRecruitmentDetail(long userId, Long recruitmentId) {
    Member member = getRequiredMember(userId);
    validateReadableRecruitment(member, recruitmentId);
    return recruitmentReader.readDetail(recruitmentId);
  }

  public List<InterviewScheduleCalendarItem> getInterviewSchedule(
      long userId, Long recruitmentId, String yearMonth) {
    Member member = getRequiredMember(userId);
    validateReadableRecruitment(member, recruitmentId);

    // 기간 파싱 및 면접 일정 조회
    YearMonth ym;
    try {
      ym = YearMonth.parse(yearMonth);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException("잘못된 연월 형식입니다. (예: 2026-03)");
    }
    LocalDateTime from = ym.atDay(1).atStartOfDay();
    LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59, 999999);

    return interviewReader.getSchedulesByRecruitmentId(recruitmentId, from, to);
  }

  // 이번 주 기준으로 권한에 맞는 면접 일정을 조회
  @Transactional(readOnly = true)
  public List<WeeklyInterviewScheduleItem> getWeeklyInterviewSchedules(
      long userId, LocalDate targetDate) {
    Member member = getRequiredMember(userId);

    LocalDate normalizedDate = targetDate != null ? targetDate : LocalDate.now();
    int daysFromSunday = normalizedDate.getDayOfWeek().getValue() % 7;
    LocalDateTime from = normalizedDate.minusDays(daysFromSunday).atStartOfDay();
    LocalDateTime to = from.plusDays(7);

    if (member.memberType() == MemberType.HR) {
      return interviewReader.getWeeklySchedules(null, from, to);
    }

    List<Long> accessibleRecruitmentIds =
        recruitmentReader.readAccessibleRecruitmentIds(userId, member.groupId());
    if (accessibleRecruitmentIds.isEmpty()) {
      return List.of();
    }

    return interviewReader.getWeeklySchedules(accessibleRecruitmentIds, from, to);
  }

  public RecruitmentDetailInfo updateRecruitment(
      long userId, Long recruitmentId, RecruitmentUpdateRequest request) {
    Member member = getRequiredMember(userId);
    validateHrMember(member, "채용 담당자만 수정이 가능합니다.");
    validateTemplateInUse(request.applicationTemplateId());

    recruitmentService.updateRecruitment(userId, recruitmentId, request);

    return recruitmentReader.readDetail(recruitmentId);
  }

  // 게시된 공고도 면접관 목록은 별도로 수정할 수 있게 처리
  public RecruitmentDetailInfo updateRecruitmentInterviewers(
      long userId, Long recruitmentId, RecruitmentInterviewersUpdateRequest request) {
    Member member = getRequiredMember(userId);
    validateHrMember(member, "채용 담당자만 면접관 설정을 수정할 수 있습니다.");

    recruitmentService.updateRecruitmentInterviewers(
        userId, recruitmentId, request.interviewerIds());

    return recruitmentReader.readDetail(recruitmentId);
  }

  // HR만 DRAFT 공고를 즉시 게시할 수 있게 처리
  public RecruitmentDetailInfo publishRecruitment(long userId, Long recruitmentId) {
    Member member = getRequiredMember(userId);
    validateHrMember(member, "채용 담당자만 채용 공고를 게시할 수 있습니다.");

    recruitmentService.publishRecruitmentNow(userId, recruitmentId);

    return recruitmentReader.readDetail(recruitmentId);
  }

  public void deleteRecruitment(long userId, Long recruitmentId) {
    Member member = getRequiredMember(userId);
    validateHrMember(member, "채용 담당자만 채용 공고를 삭제할 수 있습니다.");

    // TODO : 지원자나 진행중인 면접이 있을 경우의 검증 로직 (지원자 모듈, 면접 모듈)
    recruitmentService.deleteRecruitment(userId, recruitmentId);
  }

  // 현재 워크스페이스에서 요청한 사용자의 멤버 정보를 조회
  private Member getRequiredMember(long userId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    return member;
  }

  // HR 멤버만 허용되는 채용 공고 관리 작업인지 확인
  private void validateHrMember(Member member, String message) {
    if (member.memberType() != MemberType.HR) {
      throw new IllegalArgumentException(message);
    }
  }

  // 사용 가능한 지원서 템플릿인지 확인
  private void validateTemplateInUse(Long templateId) {
    var template =
        applicationTemplateRepository
            .findActiveById(templateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (!template.isInUse()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  // 채용 공고 읽기 권한 검증
  private void validateReadableRecruitment(Member member, Long recruitmentId) {
    if (member.memberType() == MemberType.HR) {
      return;
    }

    List<Long> accessibleRecruitmentIds =
        recruitmentReader.readAccessibleRecruitmentIds(member.userId(), member.groupId());

    if (!accessibleRecruitmentIds.contains(recruitmentId)) {
      throw new IllegalArgumentException("해당 채용 공고에 접근할 권한이 없습니다.");
    }
  }
}
