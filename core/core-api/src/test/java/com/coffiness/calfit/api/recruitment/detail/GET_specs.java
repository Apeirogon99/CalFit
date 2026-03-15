package com.coffiness.calfit.api.recruitment.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.*;
import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.*;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments/{recruitmentId}")
public class GET_specs {

  @Test
  void 채용공고_상세_조회가_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    List<RecruitmentStageRequest> stages1 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request1 =
        new RecruitmentCreateRequest(
            "시니어 백엔드 채용",
            2,
            applicationTemplateId,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            5,
            10,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages1);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request1);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.getRecruitmentDetail(token, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();

    RecruitmentDetailResponse data = response.getData();
    assertThat(data.id()).isEqualTo(recruitmentId);
    assertThat(data.title()).isEqualTo("시니어 백엔드 채용");
    assertThat(data.contents()).isEqualTo("경력직 모집합니다");
    assertThat(data.careerType()).isEqualTo(CareerType.EXPERIENCED);
    assertThat(data.targetCount()).isEqualTo(2);
    assertThat(data.applicationTemplateId()).isEqualTo(applicationTemplateId);
    assertThat(data.stages()).hasSize(4);
    assertThat(data.stages().get(0).stageName()).isEqualTo("서류 전형");
    assertThat(data.stages().get(0).stageType()).isEqualTo(RecruitmentStageType.DOCUMENT);
    assertThat(data.stages().get(1).stageName()).isEqualTo("면접 전형");
    assertThat(data.stages().get(1).stageType()).isEqualTo(RecruitmentStageType.INTERVIEW);
    assertThat(data.endDate()).isAfter(data.startDate());
  }

  @Test
  void 채용공고_상세_조회시_총_지원자와_진행중_면접_수를_함께_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired InterviewScheduleRepository interviewScheduleRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ObjectMapper objectMapper) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 테스트");

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상세 집계 테스트 공고",
            List.of(interviewer.userId()),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.of(2030, 3, 31, 18, 0),
            groupRepository,
            applicationTemplateRepository);

    createApplicantAndApply(
        applicantFixture,
        applicationFixture,
        objectMapper,
        tenantId,
        recruitment.recruitmentId(),
        recruitment.applicationStageId(),
        recruitment.applicationTemplateId());
    createApplicantAndApply(
        applicantFixture,
        applicationFixture,
        objectMapper,
        tenantId,
        recruitment.recruitmentId(),
        recruitment.applicationStageId(),
        recruitment.applicationTemplateId());

    MeetingRoomResponse meetingRoom =
        meetingRoomFixture.create(hrToken, tenantId, "상세 집계 면접실", 3, 6).getData();

    ApiResponse<InterviewResponse> interviewResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoom.id(),
            LocalDateTime.of(2030, 3, 13, 14, 0),
            60,
            "유효한 면접");

    assertThat(interviewResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    saveInterview(
        tenantId,
        interviewScheduleRepository,
        recruitment.recruitmentId(),
        recruitment.stageId(),
        meetingRoom.id(),
        LocalDateTime.of(2030, 3, 14, 10, 0),
        60,
        InterviewStatus.CANCELLED,
        "취소된 면접");
    saveInterview(
        tenantId,
        interviewScheduleRepository,
        recruitment.recruitmentId(),
        recruitment.stageId(),
        meetingRoom.id(),
        LocalDateTime.of(2024, 3, 14, 10, 0),
        60,
        InterviewStatus.COMPLETED,
        "종료된 면접");

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.getRecruitmentDetail(hrToken, tenantId, recruitment.recruitmentId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().totalApplicants()).isEqualTo(2);
    assertThat(response.getData().processingInterview()).isEqualTo(1);
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

  // 테스트용 채용 공고와 첫 면접 단계를 생성
  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      String title,
      List<Long> interviewerIds,
      LocalDateTime startDate,
      LocalDateTime endDate,
      GroupRepository groupRepository,
      ApplicationTemplateRepository applicationTemplateRepository) {
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            2,
            applicationTemplateId,
            "상세 집계 테스트 공고",
            startDate,
            endDate,
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            interviewerIds,
            List.of(
                new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    RecruitmentDetailResponse detailResponse =
        recruitmentFixture.getRecruitmentDetail(token, tenantId, recruitment.id()).getData();

    Long interviewStageId =
        detailResponse.stages().stream()
            .filter(stage -> stage.stageType() == RecruitmentStageType.INTERVIEW)
            .findFirst()
            .orElseThrow()
            .id();
    Long applicationStageId = detailResponse.stages().stream().findFirst().orElseThrow().id();

    return new RecruitmentContext(
        recruitment.id(),
        applicationStageId,
        interviewStageId,
        detailResponse.applicationTemplateId());
  }

  // 지원자를 생성하고 해당 채용 공고에 지원시킴
  private void createApplicantAndApply(
      ApplicantFixture applicantFixture,
      ApplicationFixture applicationFixture,
      ObjectMapper objectMapper,
      String tenantId,
      Long recruitmentId,
      Long stageId,
      Long applicationTemplateId) {
    String email = "app-" + UUID.randomUUID().toString().substring(0, 8) + "@t.com";
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();

    applicantFixture.signUp(tenantId, email, password, name);
    ApiResponse<ApplicantLoginResponse> loginResponse =
        applicantFixture.login(tenantId, email, password);

    ApplicationCreateRequest request =
        new ApplicationCreateRequest(
            null,
            recruitmentId,
            stageId,
            applicationTemplateId,
            name,
            Gender.MALE,
            LocalDate.of(1995, 1, 1),
            "01012345678",
            email,
            objectMapper.createObjectNode());

    ApiResponse<Void> response =
        applicationFixture.createApplication(loginResponse.getData().accessToken(), request);
    assertThat(response.getResult())
        .withFailMessage(
            "result=%s, errorCode=%s, errorMessage=%s, errorData=%s",
            response.getResult(),
            response.getError() == null ? null : response.getError().getCode(),
            response.getError() == null ? null : response.getError().getMessage(),
            response.getError() == null ? null : response.getError().getData())
        .isEqualTo(ResultType.SUCCESS);
  }

  // 제외 대상 면접 상태를 만들기 위해 테스트용 면접 레코드를 직접 저장
  private void saveInterview(
      String tenantId,
      InterviewScheduleRepository interviewScheduleRepository,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      InterviewStatus interviewStatus,
      String memo) {
    TenantContext.setTenantId(tenantId);
    try {
      interviewScheduleRepository.save(
          new InterviewScheduleEntity(
              recruitmentId,
              recruitmentStageId,
              meetingRoomId,
              scheduledAt,
              durationMinutes,
              memo,
              interviewStatus));
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("상세 조회 테스트 그룹", "#3B82F6")))
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
          .save(ApplicationTemplateEntity.create("상세 조회 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(
      Long recruitmentId, Long applicationStageId, Long stageId, Long applicationTemplateId) {}
}
