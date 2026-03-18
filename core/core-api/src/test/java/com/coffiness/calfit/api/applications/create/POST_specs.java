package com.coffiness.calfit.api.applications.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.ApplicationFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/applications")
class POST_specs {

  @Test
  void 지원자는_본인으로_지원서를_작성할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "self@test.com");

    ApiResponse<Void> response =
        applicationFixture.createApplication(
            applicant.token(),
            new ApplicationCreateRequest(
                null,
                recruitment.recruitmentId(),
                recruitment.firstStageId(),
                null,
                applicant.name(),
                Gender.MALE,
                LocalDate.of(1995, 1, 1),
                "01012345678",
                applicant.email(),
                objectMapper.createObjectNode()));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 지원자는_다른_applicantId로_지원서를_작성할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicantA =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "a@test.com");
    ApplicantContext applicantB =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "b@test.com");

    ApiResponse<Void> response =
        applicationFixture.createApplication(
            applicantA.token(),
            new ApplicationCreateRequest(
                applicantB.id(),
                recruitment.recruitmentId(),
                recruitment.firstStageId(),
                null,
                applicantA.name(),
                Gender.MALE,
                LocalDate.of(1995, 1, 1),
                "01012345678",
                applicantA.email(),
                objectMapper.createObjectNode()));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_같은_채용에_중복_지원할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "duplicate-apply@test.com");

    ApplicationCreateRequest request =
        new ApplicationCreateRequest(
            null,
            recruitment.recruitmentId(),
            recruitment.firstStageId(),
            null,
            applicant.name(),
            Gender.MALE,
            LocalDate.of(1995, 1, 1),
            "01012345678",
            applicant.email(),
            objectMapper.createObjectNode());

    ApiResponse<Void> firstResponse =
        applicationFixture.createApplication(applicant.token(), request);
    ApiResponse<Void> secondResponse =
        applicationFixture.createApplication(applicant.token(), request);

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(secondResponse.getError()).isNotNull();
  }

  @Test
  void 지원자는_마감된_채용에_지원할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired RecruitmentRepository recruitmentRepository) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    closeRecruitment(recruitment.tenantId(), recruitment.recruitmentId(), recruitmentRepository);
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "closed@test.com");

    ApiResponse<Void> response =
        applicationFixture.createApplication(
            applicant.token(),
            new ApplicationCreateRequest(
                null,
                recruitment.recruitmentId(),
                recruitment.firstStageId(),
                null,
                applicant.name(),
                Gender.MALE,
                LocalDate.of(1995, 1, 1),
                "01012345678",
                applicant.email(),
                objectMapper.createObjectNode()));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_존재하지_않는_채용에_지원할_수_없다(
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    String tenantId = workspaceFixture.createWorkspace(hrToken).getData().workspaceId();
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, tenantId, "missing-recruitment@test.com");

    ApiResponse<Void> response =
        applicationFixture.createApplication(
            applicant.token(),
            new ApplicationCreateRequest(
                null,
                999999L,
                null,
                null,
                applicant.name(),
                Gender.MALE,
                LocalDate.of(1995, 1, 1),
                "01012345678",
                applicant.email(),
                objectMapper.createObjectNode()));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  private RecruitmentContext createRecruitment(
      UserFixture userFixture,
      WorkspaceFixture workspaceFixture,
      RecruitmentFixture recruitmentFixture,
      GroupRepository groupRepository,
      ApplicationTemplateRepository applicationTemplateRepository) {
    String hrToken = userFixture.createUserAndGetToken();
    String tenantId = workspaceFixture.createWorkspace(hrToken).getData().workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(
            hrToken,
            tenantId,
            new RecruitmentCreateRequest(
                "backend-recruitment",
                1,
                applicationTemplateId,
                "backend contents",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                CareerType.EXPERIENCED,
                1,
                3,
                leadGroupId,
                List.of(),
                List.of(hrUserId),
                List.of(
                    new RecruitmentStageRequest("document", RecruitmentStageType.DOCUMENT, 1),
                    new RecruitmentStageRequest("interview", RecruitmentStageType.INTERVIEW, 2),
                    new RecruitmentStageRequest("pass", RecruitmentStageType.PASS, 3))));
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData().get(0);
    return new RecruitmentContext(tenantId, recruitment.id(), recruitment.stages().get(0).id());
  }

  private ApplicantContext signUpAndLogin(
      ApplicantFixture applicantFixture, String tenantId, String email) {
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();

    ApiResponse<ApplicantResponse> signUpResponse =
        applicantFixture.signUp(tenantId, email, password, name);
    assertThat(signUpResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantLoginResponse> loginResponse =
        applicantFixture.login(tenantId, email, password);
    assertThat(loginResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    return new ApplicantContext(
        signUpResponse.getData().id(), email, name, loginResponse.getData().accessToken());
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(
              () -> groupRepository.save(GroupEntity.create("application-test-group", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(ApplicationTemplateEntity.create("application-test-template", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private void closeRecruitment(
      String tenantId, Long recruitmentId, RecruitmentRepository recruitmentRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      RecruitmentEntity recruitment =
          recruitmentRepository.findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE).orElseThrow();
      recruitment.update(
          recruitment.getTitle(),
          RecruitmentStatus.CLOSED,
          recruitment.getTargetCount(),
          recruitment.getStartDate(),
          recruitment.getEndDate(),
          recruitment.getApplicationTemplateId(),
          recruitment.getContents(),
          recruitment.getCareerType(),
          recruitment.getMinExperienceYears(),
          recruitment.getMaxExperienceYears(),
          recruitment.getLeadGroupId());
      recruitmentRepository.save(recruitment);
    } finally {
      TenantContext.clear();
    }
  }

  private record RecruitmentContext(String tenantId, Long recruitmentId, Long firstStageId) {}

  private record ApplicantContext(Long id, String email, String name, String token) {}
}
