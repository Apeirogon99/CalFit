package com.coffiness.calfit.api.applicants.signup;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces/{workspaceId}/applicants/signup")
public class POST_specs {

  @Test
  void 지원자_회원가입에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    String email = applicantFixture.randomEmail();
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(workspaceId, email, password, name);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().email()).isEqualTo(email);
    assertThat(response.getData().workspaceId()).isEqualTo(workspaceId);
  }

  @Test
  void 지원자는_동일한_이메일로_중복_회원가입할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    String email = "dup@test.com";
    String password = applicantFixture.randomPassword();

    ApiResponse<ApplicantResponse> firstResponse =
        applicantFixture.signUp(workspaceId, email, password, "first");
    ApiResponse<ApplicantResponse> secondResponse =
        applicantFixture.signUp(workspaceId, email, password, "second");

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(secondResponse.getError()).isNotNull();
  }

  @Test
  void 지원자는_잘못된_이메일_형식으로_회원가입할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            workspaceId, "invalid-email", applicantFixture.randomPassword(), "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_비밀번호_없이_회원가입할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(workspaceId, "blank-pass@test.com", "", "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_이름_없이_회원가입할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            workspaceId, "blank-name@test.com", applicantFixture.randomPassword(), "");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 존재하지_않는_워크스페이스에는_회원가입할_수_없다(@Autowired ApplicantFixture applicantFixture) {
    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            "workspace-not-found",
            "missing-workspace@test.com",
            applicantFixture.randomPassword(),
            "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_이름_길이_제한을_초과하면_회원가입할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            workspaceId, "long-name@test.com", applicantFixture.randomPassword(), "a".repeat(51));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void 지원자는_다른_워크스페이스에서_같은_이메일로_가입할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String firstHrToken = userFixture.createUserAndGetToken();
    String secondHrToken = userFixture.createUserAndGetToken();
    String firstWorkspaceId =
        workspaceFixture.createWorkspace(firstHrToken).getData().workspaceId();
    String secondWorkspaceId =
        workspaceFixture.createWorkspace(secondHrToken).getData().workspaceId();

    String email = "shared-applicant@test.com";
    String password = applicantFixture.randomPassword();

    ApiResponse<ApplicantResponse> firstResponse =
        applicantFixture.signUp(firstWorkspaceId, email, password, "first-applicant");
    ApiResponse<ApplicantResponse> secondResponse =
        applicantFixture.signUp(secondWorkspaceId, email, password, "second-applicant");

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(firstResponse.getData().workspaceId()).isEqualTo(firstWorkspaceId);
    assertThat(secondResponse.getData().workspaceId()).isEqualTo(secondWorkspaceId);
  }
}
