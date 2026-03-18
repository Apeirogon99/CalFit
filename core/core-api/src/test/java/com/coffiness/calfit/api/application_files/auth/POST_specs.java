package com.coffiness.calfit.api.application_files.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.ApplicationFileFixture;
import com.coffiness.calfit.api.fixture.ApplicationFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@CalfitApiTest
@DisplayName("POST /api/v1/application-files/presign-upload auth")
class POST_specs {

  @MockBean private S3Presigner presigner;

  @Test
  void 지원자는_본인_지원서에_파일_업로드를_요청할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationFileFixture applicationFileFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationFileRepository applicationFileRepository) {
    stubPresigner();
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-self@test.com");
    Long applicationId =
        createApplication(
            recruitment, applicant, applicationFixture, objectMapper, applicationRepository);

    ResponseEntity<PresignUploadResponse> response =
        applicationFileFixture.presignUpload(
            applicant.token(),
            recruitment.tenantId(),
            new PresignUploadRequest(
                applicationId, applicant.id(), "resume", "resume.pdf", "application/pdf"));

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().uploadUrl()).isEqualTo("https://example.com/upload");

    List<ApplicationFileEntity> files =
        withTenant(
            recruitment.tenantId(),
            () ->
                applicationFileRepository.findByApplicationIdAndStatusAndUploadStatus(
                    applicationId, EntityStatus.ACTIVE, UploadStatus.PENDING));
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getApplicantId()).isEqualTo(applicant.id());
  }

  @Test
  void 지원자는_다른_지원자의_지원서로_파일_업로드를_요청할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationFileFixture applicationFileFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ApplicationRepository applicationRepository) {
    stubPresigner();
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicantA =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-a@test.com");
    ApplicantContext applicantB =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-b@test.com");
    Long otherApplicationId =
        createApplication(
            recruitment, applicantB, applicationFixture, objectMapper, applicationRepository);

    ResponseEntity<String> response =
        applicationFileFixture.presignUploadRaw(
            applicantA.token(),
            recruitment.tenantId(),
            new PresignUploadRequest(
                otherApplicationId, applicantB.id(), "resume", "resume.pdf", "application/pdf"));

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }

  @Test
  void 요청의_applicantId가_실제_지원서_소유자와_다르면_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationFileFixture applicationFileFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ApplicationRepository applicationRepository) {
    stubPresigner();
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    ApplicantContext applicantA =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-owner@test.com");
    ApplicantContext applicantB =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-mismatch@test.com");
    Long applicationId =
        createApplication(
            recruitment, applicantA, applicationFixture, objectMapper, applicationRepository);

    ResponseEntity<String> response =
        applicationFileFixture.presignUploadRaw(
            applicantA.token(),
            recruitment.tenantId(),
            new PresignUploadRequest(
                applicationId, applicantB.id(), "resume", "resume.pdf", "application/pdf"));

    assertThat(response.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  void 지원자가_아닌_요청자는_파일_업로드를_요청할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationFileFixture applicationFileFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ApplicationRepository applicationRepository) {
    stubPresigner();
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);
    String hrToken = userFixture.createUserAndGetToken();
    workspaceFixture.createWorkspace(hrToken);
    ApplicantContext applicant =
        signUpAndLogin(applicantFixture, recruitment.tenantId(), "file-hr@test.com");
    Long applicationId =
        createApplication(
            recruitment, applicant, applicationFixture, objectMapper, applicationRepository);

    ResponseEntity<String> response =
        applicationFileFixture.presignUploadRaw(
            hrToken,
            recruitment.tenantId(),
            new PresignUploadRequest(
                applicationId, applicant.id(), "resume", "resume.pdf", "application/pdf"));

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }

  private void stubPresigner() {
    PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
    try {
      when(presigned.url()).thenReturn(new URL("https://example.com/upload"));
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
    when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
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
                "file-auth-recruitment",
                1,
                applicationTemplateId,
                "file auth contents",
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

  private Long createApplication(
      RecruitmentContext recruitment,
      ApplicantContext applicant,
      ApplicationFixture applicationFixture,
      ObjectMapper objectMapper,
      ApplicationRepository applicationRepository) {
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

    List<ApplicationEntity> applications =
        withTenant(
            recruitment.tenantId(),
            () ->
                applicationRepository.findByRecruitmentIdAndStatus(
                    recruitment.recruitmentId(), EntityStatus.ACTIVE));

    return applications.stream()
        .filter(application -> application.getApplicantId().equals(applicant.id()))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    return withTenant(
        tenantId,
        () ->
            groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
                .findFirst()
                .orElseGet(
                    () ->
                        groupRepository.save(
                            GroupEntity.create("application-file-group", "#3B82F6")))
                .getId());
  }

  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    return withTenant(
        tenantId,
        () ->
            applicationTemplateRepository
                .save(ApplicationTemplateEntity.create("application-file-template", "{}", true))
                .getId());
  }

  private <T> T withTenant(String tenantId, java.util.function.Supplier<T> supplier) {
    TenantContext.setTenantId(tenantId);
    try {
      return supplier.get();
    } finally {
      TenantContext.clear();
    }
  }

  private record RecruitmentContext(String tenantId, Long recruitmentId, Long firstStageId) {}

  private record ApplicantContext(Long id, String email, String name, String token) {}
}
