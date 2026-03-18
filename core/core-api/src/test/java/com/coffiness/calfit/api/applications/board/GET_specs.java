package com.coffiness.calfit.api.applications.board;

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
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
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
@DisplayName("GET /api/v1/applications/board")
public class GET_specs {

  @Test
  void recruitmentId로_칸반_보드를_조회할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ObjectMapper objectMapper,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange: HR 계정 + 워크스페이스
    String hrToken = userFixture.createUserAndGetToken();
    var workspace = workspaceFixture.createWorkspace(hrToken).getData();
    String tenantId = workspace.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest recruitmentRequest =
        new RecruitmentCreateRequest(
            "백엔드 채용",
            2,
            applicationTemplateId,
            "백엔드 채용 공고",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            5,
            leadGroupId,
            List.of(),
            List.of(hrUserId),
            stages);

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(hrToken, tenantId, recruitmentRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<RecruitmentListResponse>> listResponse =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId);
    Long recruitmentId = listResponse.getData().get(0).id();
    Long processId = listResponse.getData().get(0).stages().get(0).id();

    // Arrange: 지원자 회원가입/로그인
    String email = "board@test.com";
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();
    applicantFixture.signUp(tenantId, email, password, name);
    ApiResponse<ApplicantLoginResponse> loginResponse =
        applicantFixture.login(tenantId, email, password);
    String applicantToken = loginResponse.getData().accessToken();

    ApplicationCreateRequest applicationRequest =
        new ApplicationCreateRequest(
            null,
            recruitmentId,
            processId,
            null,
            name,
            Gender.MALE,
            LocalDate.of(1995, 1, 1),
            "01012345678",
            email,
            objectMapper.createObjectNode());

    ApiResponse<?> createApplicationResponse =
        applicationFixture.createApplication(applicantToken, applicationRequest);
    assertThat(createApplicationResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    ApiResponse<KanbanBoard> response =
        applicationFixture.getKanbanBoard(hrToken, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().columns().size()).isEqualTo(4);
    assertThat(response.getData().columns().get(0).applications().size()).isEqualTo(1);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("board-test-group", "#3B82F6")))
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
          .save(ApplicationTemplateEntity.create("board-test-template", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
