package com.coffiness.calfit.api.recruitment.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
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

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments")
public class GET_specs {

  @Test
  void 채용공고_목록_조회에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
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

    List<RecruitmentStageRequest> stages2 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("처우 협의", RecruitmentStageType.OFFER, 3),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4));

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
    RecruitmentCreateRequest request2 =
        new RecruitmentCreateRequest(
            "신입 프론트엔드 채용",
            3,
            applicationTemplateId,
            "신입 모집합니다.",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages2);

    ApiResponse<Void> createResponse1 =
        recruitmentFixture.createRecruitment(token, tenantId, request1);
    ApiResponse<Void> createResponse2 =
        recruitmentFixture.createRecruitment(token, tenantId, request2);

    assertThat(createResponse1.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(createResponse2.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    ApiResponse<List<RecruitmentListResponse>> response =
        recruitmentFixture.getRecruitmentList(token, tenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData())
        .extracting(RecruitmentListResponse::title)
        .contains("시니어 백엔드 채용", "신입 프론트엔드 채용");
  }

  @Test
  void 채용공고_목록은_담당조직원_참조조직원_등록면접관에게만_노출된다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    Long leadGroupId =
        memberFixture.createGroup("담당 조직", "#14B8A6", hrToken, tenantId).getData().id();
    Long referenceGroupId =
        memberFixture.createGroup("참조 조직", "#3B82F6", hrToken, tenantId).getData().id();
    Long unrelatedGroupId =
        memberFixture.createGroup("무관 조직", "#F59E0B", hrToken, tenantId).getData().id();

    MemberAccessContext leadMember =
        createWorkspaceMember(memberFixture, userFixture, context, "lead-member", null);
    MemberAccessContext referenceMember =
        createWorkspaceMember(memberFixture, userFixture, context, "reference-member", null);
    MemberAccessContext interviewerMember =
        createWorkspaceMember(
            memberFixture, userFixture, context, "interviewer-member", unrelatedGroupId);
    MemberAccessContext unrelatedMember =
        createWorkspaceMember(
            memberFixture, userFixture, context, "unrelated-member", unrelatedGroupId);

    memberFixture.assignGroup(leadMember.memberId(), leadGroupId, hrToken, tenantId);
    memberFixture.assignGroup(referenceMember.memberId(), referenceGroupId, hrToken, tenantId);

    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "접근 범위 확인용 채용",
            2,
            applicationTemplateId,
            "노출 대상 확인",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(referenceGroupId),
            List.of(interviewerMember.userId()),
            List.of(
                new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(hrToken, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    assertThat(recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData())
        .extracting(RecruitmentListResponse::title)
        .contains("접근 범위 확인용 채용");

    assertThat(recruitmentFixture.getRecruitmentList(leadMember.token(), tenantId).getData())
        .extracting(RecruitmentListResponse::title)
        .contains("접근 범위 확인용 채용");

    assertThat(recruitmentFixture.getRecruitmentList(referenceMember.token(), tenantId).getData())
        .extracting(RecruitmentListResponse::title)
        .contains("접근 범위 확인용 채용");

    assertThat(recruitmentFixture.getRecruitmentList(interviewerMember.token(), tenantId).getData())
        .extracting(RecruitmentListResponse::title)
        .contains("접근 범위 확인용 채용");

    assertThat(recruitmentFixture.getRecruitmentList(unrelatedMember.token(), tenantId).getData())
        .extracting(RecruitmentListResponse::title)
        .doesNotContain("접근 범위 확인용 채용");
  }

  @Test
  void 채용공고_목록_조회시_전형_단계별_지원자_수가_함께_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "단계별 지원자 수 확인용 채용",
            2,
            applicationTemplateId,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.title().equals("단계별 지원자 수 확인용 채용"))
            .findFirst()
            .orElseThrow();

    Long documentStageId = recruitment.stages().get(0).id();
    Long interviewStageId = recruitment.stages().get(1).id();

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        documentStageId,
        applicationTemplateId,
        1001L,
        "지원자 하나",
        "applicant1001@coffiness.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        documentStageId,
        applicationTemplateId,
        1002L,
        "지원자 둘",
        "applicant1002@coffiness.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        interviewStageId,
        applicationTemplateId,
        1003L,
        "지원자 셋",
        "applicant1003@coffiness.com");

    // Act
    RecruitmentListResponse response =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.id().equals(recruitment.id()))
            .findFirst()
            .orElseThrow();

    // Assert
    assertThat(response.stages())
        .anySatisfy(
            stage -> {
              if ("서류 전형".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(2);
              }
            })
        .anySatisfy(
            stage -> {
              if ("실무 면접".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(1);
              }
            })
        .anySatisfy(
            stage -> {
              if ("최종 합격".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(0);
              }
            });
  }

  @Test
  void 채용공고_목록에서는_불합격_단계가_노출되지_않는다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "불합격 단계 비노출 확인용 채용",
            2,
            applicationTemplateId,
            "목록 전형 단계 확인",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            List.of(
                new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 면접", RecruitmentStageType.INTERVIEW, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    RecruitmentListResponse response =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.title().equals("불합격 단계 비노출 확인용 채용"))
            .findFirst()
            .orElseThrow();

    // Assert
    assertThat(response.stages()).extracting(stage -> stage.stageName()).doesNotContain("불합격");
    assertThat(response.stages()).extracting(stage -> stage.stageName()).endsWith("최종 합격");
  }

  @Test
  void 채용공고_목록_조회시_담당_조직과_참조_조직_ID가_정확히_반환된다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    List<Long> referenceGroupIds = createReferenceGroupIds(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "조직 매핑 확인용 채용",
            2,
            applicationTemplateId,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            referenceGroupIds,
            List.of(interviewerId),
            List.of(
                new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    RecruitmentListResponse response =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.title().equals("조직 매핑 확인용 채용"))
            .findFirst()
            .orElseThrow();

    // Assert
    assertThat(response.leadGroupId()).isEqualTo(leadGroupId);
    assertThat(response.referenceGroupIds()).containsExactlyInAnyOrderElementsOf(referenceGroupIds);
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("목록 조회 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 참조 조직 테스트용 그룹 ID를 생성
  private List<Long> createReferenceGroupIds(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      GroupEntity first = groupRepository.save(GroupEntity.create("목록 참조 조직 A", "#10B981"));
      GroupEntity second = groupRepository.save(GroupEntity.create("목록 참조 조직 B", "#F59E0B"));
      return List.of(first.getId(), second.getId());
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
          .save(ApplicationTemplateEntity.create("목록 조회 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private MemberAccessContext createWorkspaceMember(
      MemberFixture memberFixture,
      UserFixture userFixture,
      MemberFixture.WorkspaceContext context,
      String emailPrefix,
      Long groupId) {
    String email = emailPrefix + "-" + userFixture.randomEmail();
    String password = userFixture.randomPassword();
    userFixture.signUp(email, password, userFixture.randomName());

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(
            context.hrToken(), context.workspaceId(), email, MemberType.INTERVIEWER);

    ApiResponse<LoginResponse> loginResponse = userFixture.login(email, password);
    String token = loginResponse.getData().accessToken();
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);

    MemberResponse member = memberFixture.getMyMember(token, context.workspaceId()).getData();
    if (groupId != null) {
      memberFixture.assignGroup(member.id(), groupId, context.hrToken(), context.workspaceId());
      member = memberFixture.getMyMember(token, context.workspaceId()).getData();
    }

    return new MemberAccessContext(token, member.userId(), member.id());
  }

  private record MemberAccessContext(String token, Long userId, Long memberId) {}
}
