package com.coffiness.calfit.api.interview.pending;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewPendingStageResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/*
 * 면접 단계별 대기 지원자 조회 API 동작을 검증
 * */
@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/interviews/pending-applicants")
class GET_specs {

  @Test
  void 면접_단계별로_면접이_안잡힌_지원자만_조회한다(
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

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository,
            hrToken,
            tenantId,
            "면접 대기 지원자 조회 공고",
            List.of(interviewer.userId()));

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.firstInterviewStageId(),
        recruitment.applicationTemplateId(),
        701L,
        "실무 대기 1",
        "first701@test.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.firstInterviewStageId(),
        recruitment.applicationTemplateId(),
        702L,
        "실무 대기 2",
        "first702@test.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.recruitmentId(),
        recruitment.finalInterviewStageId(),
        recruitment.applicationTemplateId(),
        703L,
        "최종 대기 1",
        "final703@test.com");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "A회의실", 3, 6).getData().id();

    ApiResponse<InterviewResponse> createdInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.firstInterviewStageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(701L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 10, 0),
            60,
            "실무 면접");

    assertThat(createdInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<InterviewPendingStageResponse>> response =
        interviewFixture.getPendingApplicants(hrToken, tenantId, recruitment.recruitmentId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(2);
    assertThat(response.getData().get(0).stageName()).isEqualTo("실무 면접");
    assertThat(response.getData().get(0).pendingApplicantCount()).isEqualTo(1);
    assertThat(response.getData().get(0).applicants())
        .extracting(applicant -> applicant.applicantId())
        .containsExactly(702L);
    assertThat(response.getData().get(1).stageName()).isEqualTo("최종 면접");
    assertThat(response.getData().get(1).pendingApplicantCount()).isEqualTo(1);
    assertThat(response.getData().get(1).applicants())
        .extracting(applicant -> applicant.applicantId())
        .containsExactly(703L);
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

  // 두 개의 면접 단계를 가진 테스트용 채용 공고를 생성
  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      GroupRepository groupRepository,
      ApplicationTemplateRepository applicationTemplateRepository,
      String token,
      String tenantId,
      String title,
      List<Long> interviewerIds) {
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            1,
            applicationTemplateId,
            "채용 공고",
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            interviewerIds,
            List.of(
                new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 면접", RecruitmentStageType.INTERVIEW, 3)));

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

    return new RecruitmentContext(
        recruitment.id(),
        recruitment.stages().get(1).id(),
        recruitment.stages().get(2).id(),
        applicationTemplateId);
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository
          .findByStatus(com.coffiness.calfit.core.enums.EntityStatus.ACTIVE)
          .stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("면접 대기 테스트 그룹", "#3B82F6")))
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
          .save(ApplicationTemplateEntity.create("면접 대기 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(
      Long recruitmentId,
      Long firstInterviewStageId,
      Long finalInterviewStageId,
      Long applicationTemplateId) {}
}
