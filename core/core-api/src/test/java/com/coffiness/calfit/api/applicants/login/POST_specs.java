package com.coffiness.calfit.api.applicants.login;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces/{workspaceId}/applicants/login")
public class POST_specs {
  @Test
  void 지원자_로그인에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    String email = applicantFixture.randomEmail();
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();
    ApiResponse<ApplicantResponse> signUp =
        applicantFixture.signUp(workspaceId, email, password, name);
    assertThat(signUp.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantLoginResponse> response =
        applicantFixture.login(workspaceId, email, password);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().accessToken()).isNotBlank();
  }

  @Test
  void 가입되지_않은_이메일로는_로그인할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantLoginResponse> response =
        applicantFixture.login(workspaceId, "missing@test.com", "pass1234");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 비밀번호가_일치하지_않으면_로그인할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> signUp =
        applicantFixture.signUp(workspaceId, "login@test.com", "pass1234", "applicant");
    assertThat(signUp.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantLoginResponse> response =
        applicantFixture.login(workspaceId, "login@test.com", "wrong-pass");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 다른_워크스페이스에서는_로그인할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> firstWorkspace = workspaceFixture.createWorkspace(hrToken);
    ApiResponse<WorkspaceResponse> secondWorkspace = workspaceFixture.createWorkspace(hrToken);

    String email = "cross-workspace@test.com";
    String password = applicantFixture.randomPassword();

    ApiResponse<ApplicantResponse> signUp =
        applicantFixture.signUp(
            firstWorkspace.getData().workspaceId(), email, password, "applicant");
    assertThat(signUp.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantLoginResponse> response =
        applicantFixture.login(secondWorkspace.getData().workspaceId(), email, password);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }
}
