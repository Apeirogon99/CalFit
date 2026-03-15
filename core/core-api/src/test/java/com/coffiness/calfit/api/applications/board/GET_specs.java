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
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.application.KanbanBoard;
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
      @Autowired ObjectMapper objectMapper) {

    // Arrange: HR 계정 + 워크스페이스
    String hrToken = userFixture.createUserAndGetToken();
    var workspace = workspaceFixture.createWorkspace(hrToken).getData();
    String tenantId = workspace.workspaceId();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest recruitmentRequest =
        new RecruitmentCreateRequest(
            "백엔드 채용",
            2,
            1L,
            "백엔드 채용 공고",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            5,
            1L,
            List.of(),
            List.of(),
            stages);

    recruitmentFixture.createRecruitment(hrToken, tenantId, recruitmentRequest);

    ApiResponse<List<RecruitmentListResponse>> listResponse =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId);
    Long recruitmentId = listResponse.getData().get(0).id();
    Long processId = listResponse.getData().get(0).stages().get(0).id();

    // Arrange: 지원자 회원가입/로그인
    String email = applicantFixture.randomEmail();
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
            1L,
            name,
            Gender.MALE,
            LocalDate.of(1995, 1, 1),
            "01012345678",
            email,
            objectMapper.createObjectNode());

    applicationFixture.createApplication(applicantToken, applicationRequest);

    // Act
    ApiResponse<KanbanBoard> response =
        applicationFixture.getKanbanBoard(hrToken, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().columns().size()).isEqualTo(3);
    assertThat(response.getData().columns().get(0).applications().size()).isEqualTo(1);
  }
}
