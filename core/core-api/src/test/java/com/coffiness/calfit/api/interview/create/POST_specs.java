package com.coffiness.calfit.api.interview.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.*;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.email.InterviewScheduleEmailMessage;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/interviews")
class POST_specs {

  @MockBean private EmailService emailService;

  @Test
  void 토큰이_없으면_생성에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewResponse> response =
        interviewFixture.create(
            null,
            tenantId,
            1L,
            1L,
            InterviewRound.FIRST,
            List.of(1L),
            List.of(1L),
            1L,
            LocalDateTime.now().plusDays(1),
            60,
            "memo");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 채용_단계가_없으면_생성에_실패한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    면접관정보 interviewer = 면접관을_초대한다(memberFixture, userFixture, hrToken, tenantId, "면접관 테스트");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    채용공고정보 recruitment =
        채용공고를_생성한다(
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository,
            hrToken,
            tenantId,
            "백엔드 채용 공고",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    ApiResponse<InterviewResponse> response =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            null,
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 14, 0),
            60,
            "백엔드 1차 면접");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접_일정을_생성하면_면접관의_내_일정에서도_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired NotificationFixture notificationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    면접관정보 interviewer = 면접관을_초대한다(memberFixture, userFixture, hrToken, tenantId, "면접관 테스트");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    채용공고정보 recruitment =
        채용공고를_생성한다(
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository,
            hrToken,
            tenantId,
            "백엔드 채용 공고",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.stageId(),
        recruitment.applicationTemplateId(),
        101L,
        "지원자 테스트",
        "applicant101@test.com");

    LocalDateTime scheduledAt = LocalDateTime.of(2030, 3, 13, 14, 0);
    ApiResponse<InterviewResponse> createResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoomId,
            scheduledAt,
            60,
            "백엔드 1차 면접");

    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<com.coffiness.calfit.v1.response.ScheduleResponse>> scheduleResponse =
        calendarFixture.getSchedules(interviewer.token(), tenantId, "2030-03-13", "2030-03-13");

    assertThat(scheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(scheduleResponse.getData()).hasSize(1);
    assertThat(scheduleResponse.getData().get(0).title()).isEqualTo("백엔드 채용 공고 · 실무 면접");
    assertThat(scheduleResponse.getData().get(0).type()).isEqualTo(ScheduleType.INTERVIEW);
    assertThat(scheduleResponse.getData().get(0).interviewScheduleId())
        .isEqualTo(createResponse.getData().id());

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(
            interviewer.token(), tenantId, scheduleResponse.getData().get(0).id());

    assertThat(detailResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.INTERVIEW);
    assertThat(detailResponse.getData().location()).isEqualTo("면접실 A");
    assertThat(detailResponse.getData().description()).isEqualTo("백엔드 1차 면접");
    assertThat(detailResponse.getData().applicantName()).isEqualTo("지원자#101");
    assertThat(detailResponse.getData().interviewScheduleId())
        .isEqualTo(createResponse.getData().id());

    assertThat(notificationFixture.unreadCount(hrToken, tenantId).getData().unreadCount()).isOne();
    assertThat(
            notificationFixture.unreadCount(interviewer.token(), tenantId).getData().unreadCount())
        .isOne();
  }

  @Test
  void 이미_면접이_잡힌_지원자는_같은_단계에_다시_배정할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    면접관정보 interviewer = 면접관을_초대한다(memberFixture, userFixture, hrToken, tenantId, "면접관 테스트");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    채용공고정보 recruitment =
        채용공고를_생성한다(
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository,
            hrToken,
            tenantId,
            "플랫폼 채용 공고",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.stageId(),
        recruitment.applicationTemplateId(),
        202L,
        "대기 지원자",
        "pending202@test.com");

    ApiResponse<InterviewResponse> firstResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(202L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 10, 0),
            60,
            "1차 면접");

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewResponse> secondResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(202L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 12, 0),
            60,
            "중복 면접");

    assertThat(secondResponse.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접_일정이_생성되면_지원자에게_날짜_시간_위치_안내_메일을_보낸다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    면접관정보 interviewer = 면접관을_초대한다(memberFixture, userFixture, hrToken, tenantId, "숨겨져야 할 면접관");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 B", 3, 6).getData().id();

    채용공고정보 recruitment =
        채용공고를_생성한다(
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository,
            hrToken,
            tenantId,
            "플랫폼 채용 공고",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.stageId(),
        recruitment.applicationTemplateId(),
        303L,
        "메일 지원자",
        "applicant303@test.com");

    ApiResponse<InterviewResponse> response =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(303L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 14, 16, 0),
            90,
            "면접 메일 테스트");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    verify(emailService, timeout(3000))
        .sendInterviewScheduleEmail(
            anyString(),
            org.mockito.ArgumentMatchers.argThat(
                (InterviewScheduleEmailMessage message) ->
                    message != null
                        && "applicant303@test.com".equals(message.getApplicantEmail())
                        && "메일 지원자".equals(message.getApplicantName())
                        && "면접실 B".equals(message.getLocation())
                        && message.getScheduledAt().equals(LocalDateTime.of(2030, 3, 14, 16, 0))
                        && message.getEndAt().equals(LocalDateTime.of(2030, 3, 14, 17, 30))
                        && !message.getLocation().contains("숨겨져야 할 면접관")));
  }

  // 테스트용 면접관 사용자를 생성하고 워크스페이스에 초대
  private 면접관정보 면접관을_초대한다(
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

    return new 면접관정보(userId, token);
  }

  // 테스트용 채용 공고와 면접 단계를 생성
  private 채용공고정보 채용공고를_생성한다(
      RecruitmentFixture recruitmentFixture,
      GroupRepository groupRepository,
      ApplicationTemplateRepository applicationTemplateRepository,
      String token,
      String tenantId,
      String title,
      List<Long> interviewerIds,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
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
            List.of(),
            interviewerIds,
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult())
        .withFailMessage(
            "result=%s, errorCode=%s, errorMessage=%s, errorData=%s",
            createResponse.getResult(),
            createResponse.getError() == null ? null : createResponse.getError().getCode(),
            createResponse.getError() == null ? null : createResponse.getError().getMessage(),
            createResponse.getError() == null ? null : createResponse.getError().getData())
        .isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    return new 채용공고정보(recruitment.id(), recruitment.stages().get(0).id(), applicationTemplateId);
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("면접 생성 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 지원서 템플릿을 생성
  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(ApplicationTemplateEntity.create("면접 생성 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private record 면접관정보(Long userId, String token) {}

  private record 채용공고정보(Long recruitmentId, Long stageId, Long applicationTemplateId) {}
}
