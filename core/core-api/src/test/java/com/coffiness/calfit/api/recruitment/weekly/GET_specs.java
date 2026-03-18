package com.coffiness.calfit.api.recruitment.weekly;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.*;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WeeklyInterviewScheduleResponse;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/*
 * 이번 주 면접 일정 조회 API 동작을 검증
 * */
@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/recruitments/weekly-interview-schedules")
class GET_specs {

  // HR 사용자는 워크스페이스의 이번 주 면접 일정을 모두 확인
  @Test
  void HR은_이번주_면접_일정을_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    Long leadGroupId =
        memberFixture.createGroup("주간 조회 담당 조직", "#14B8A6", hrToken, tenantId).getData().id();

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "A회의실", 3, 6).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상반기 백엔드 채용",
            applicationTemplateId,
            leadGroupId,
            List.of(),
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));
    Long firstApplicantId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);
    Long nextWeekApplicantId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);

    ApiResponse<?> firstInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(firstApplicantId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 10, 0),
            60,
            "1차 실무 면접");
    assertThat(firstInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<?> nextWeekInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(nextWeekApplicantId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 18, 14, 0),
            60,
            "다음 주 면접");
    assertThat(nextWeekInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            hrToken, tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).title()).isEqualTo("상반기 백엔드 채용 · 실무 면접");
    assertThat(response.getData().get(0).interviewerName()).isEqualTo("면접관 A");
    assertThat(response.getData().get(0).applicantName()).startsWith("Applicant-");
    assertThat(response.getData().get(0).location()).isEqualTo("A회의실 (3층)");
    assertThat(response.getData().get(0).description()).isEqualTo("1차 실무 면접");
  }

  @Test
  void 면접관은_접근가능한_공고의_이번주_면접_일정을_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewerA =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");
    InterviewerContext interviewerB =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 B");
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    Long leadGroupId =
        memberFixture.createGroup("면접 주간 담당 조직", "#14B8A6", hrToken, tenantId).getData().id();

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "B회의실", 5, 8).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상반기 프론트 채용",
            applicationTemplateId,
            leadGroupId,
            List.of(),
            List.of(interviewerA.userId(), interviewerB.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));
    Long applicantAId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);
    Long applicantBId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);

    ApiResponse<?> interviewerAInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewerA.userId()),
            List.of(applicantAId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 9, 0),
            60,
            "A 면접");
    assertThat(interviewerAInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<?> interviewerBInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewerB.userId()),
            List.of(applicantBId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 14, 11, 0),
            60,
            "B 면접");
    assertThat(interviewerBInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            interviewerA.token(), tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(2);
    assertThat(response.getData())
        .extracting(WeeklyInterviewScheduleResponse::interviewerName)
        .containsExactly("면접관 A", "면접관 B");
    assertThat(response.getData())
        .extracting(WeeklyInterviewScheduleResponse::description)
        .containsExactly("A 면접", "B 면접");
  }

  @Test
  void 불완전한_면접_레코드는_이번주_면접_목록에서_제외한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired InterviewScheduleRepository interviewScheduleRepository,
      @Autowired InterviewScheduleInterviewerRepository interviewScheduleInterviewerRepository,
      @Autowired InterviewScheduleApplicantRepository interviewScheduleApplicantRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    Long leadGroupId =
        memberFixture.createGroup("불완전 레코드 담당 조직", "#14B8A6", hrToken, tenantId).getData().id();

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "C회의실", 3, 6).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상반기 QA 채용",
            applicationTemplateId,
            leadGroupId,
            List.of(),
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));
    Long validApplicantId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);

    ApiResponse<?> validInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(validApplicantId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 12, 0),
            120,
            "정상 면접");
    assertThat(validInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    InterviewScheduleEntity invalidInterview =
        saveInvalidInterview(
            tenantId,
            interviewScheduleRepository,
            interviewScheduleInterviewerRepository,
            interviewScheduleApplicantRepository,
            recruitment.recruitmentId(),
            meetingRoomId,
            interviewer.userId());

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            hrToken, tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).title()).isEqualTo("상반기 QA 채용 · 실무 면접");
    assertThat(response.getData().get(0).description()).isEqualTo("정상 면접");
  }

  @Test
  void 권한이_없는_멤버는_이번주_면접_일정을_조회해도_0건이다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    Long leadGroupId =
        memberFixture.createGroup("담당 조직", "#14B8A6", hrToken, tenantId).getData().id();
    Long unrelatedGroupId =
        memberFixture.createGroup("무관 조직", "#F59E0B", hrToken, tenantId).getData().id();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");
    MemberAccessContext unrelatedMember =
        createWorkspaceMember(memberFixture, userFixture, context, "weekly-unrelated");
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);

    memberFixture.assignGroup(unrelatedMember.memberId(), unrelatedGroupId, hrToken, tenantId);

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "D회의실", 5, 8).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "접근 권한 확인용 채용",
            applicationTemplateId,
            leadGroupId,
            List.of(),
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));
    Long applicantId =
        createApplicantAndApply(
            applicantFixture,
            applicationRepository,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            applicationTemplateId);

    ApiResponse<?> interviewResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(applicantId),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 15, 0),
            60,
            "권한 확인용 면접");
    assertThat(interviewResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            unrelatedMember.token(), tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }

  // 단계 정보가 없는 면접 레코드를 현재 tenant에 직접 저장
  private InterviewScheduleEntity saveInvalidInterview(
      String tenantId,
      InterviewScheduleRepository interviewScheduleRepository,
      InterviewScheduleInterviewerRepository interviewScheduleInterviewerRepository,
      InterviewScheduleApplicantRepository interviewScheduleApplicantRepository,
      Long recruitmentId,
      Long meetingRoomId,
      Long interviewerUserId) {
    TenantContext.setTenantId(tenantId);
    try {
      InterviewScheduleEntity invalidInterview =
          interviewScheduleRepository.save(
              new InterviewScheduleEntity(
                  recruitmentId,
                  null,
                  meetingRoomId,
                  LocalDateTime.of(2030, 3, 13, 12, 0),
                  120,
                  "잘못된 면접",
                  InterviewStatus.CONFIRMED));
      interviewScheduleInterviewerRepository.save(
          new InterviewScheduleInterviewerEntity(invalidInterview.getId(), interviewerUserId));
      interviewScheduleApplicantRepository.save(
          new InterviewScheduleApplicantEntity(invalidInterview.getId(), 402L));
      return invalidInterview;
    } finally {
      TenantContext.clear();
    }
  }

  // 테스트용 면접관 사용자를 생성하고 워크스페이스에 초대
  private InterviewerContext inviteInterviewer(
      MemberFixture memberFixture,
      UserFixture userFixture,
      String hrToken,
      String tenantId,
      String name) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    Long userId = userFixture.signUp(email, password, name).getData().id();
    String token = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);

    return new InterviewerContext(userId, token);
  }

  // 면접 일정 조회에 사용할 채용 공고와 면접 단계를 생성
  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      String title,
      Long applicationTemplateId,
      Long leadGroupId,
      List<Long> referenceGroupIds,
      List<Long> interviewerIds,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            1,
            applicationTemplateId,
            "채용 공고",
            startDate,
            endDate,
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            referenceGroupIds,
            interviewerIds,
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    Long stageId = recruitment.stages().get(0).id();
    return new RecruitmentContext(recruitment.id(), stageId);
  }

  private Long createApplicantAndApply(
      ApplicantFixture applicantFixture,
      ApplicationRepository applicationRepository,
      String tenantId,
      Long recruitmentId,
      Long stageId,
      Long applicationTemplateId) {
    String email = "weekly-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@t.com";
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();

    ApiResponse<ApplicantResponse> signUpResponse =
        applicantFixture.signUp(tenantId, email, password, name);
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitmentId,
        stageId,
        applicationTemplateId,
        signUpResponse.getData().id(),
        name,
        email);

    return signUpResponse.getData().id();
  }

  private Long createApplicationTemplate(
      String token, String tenantId, ApplicationTemplateFixture applicationTemplateFixture) {
    return applicationTemplateFixture.createUsedTemplateId(token, tenantId, "주간 조회 테스트 템플릿");
  }

  // 테스트용 일반 멤버 사용자를 생성하고 워크스페이스에 초대
  private MemberAccessContext createWorkspaceMember(
      MemberFixture memberFixture,
      UserFixture userFixture,
      MemberFixture.WorkspaceContext context,
      String emailPrefix) {
    String email = emailPrefix + "-" + userFixture.randomEmail();
    String password = userFixture.randomPassword();
    Long userId = userFixture.signUp(email, password, userFixture.randomName()).getData().id();
    String token = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(
            context.hrToken(), context.workspaceId(), email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);
    MemberResponse memberResponse =
        memberFixture.getMyMember(token, context.workspaceId()).getData();
    return new MemberAccessContext(token, userId, memberResponse.id());
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(Long recruitmentId, Long stageId) {}

  private record MemberAccessContext(String token, Long userId, Long memberId) {}
}
