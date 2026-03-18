package com.coffiness.calfit.api.applications.template.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.ApplicationTemplateListResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces/{workspaceId}/application-templates")
public class POST_specs {

  @Test
  void 인사담당자는_지원서_템플릿을_생성할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    // Arrange
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    Map<String, Object> request =
        applicationTemplateFixture.newCreateRequest(
            "backend-default-template",
            List.of(
                Map.of(
                    "key", "portfolioUrl",
                    "label", "Portfolio URL",
                    "type", "TEXT")),
            true);

    // Act
    ApiResponse<ApplicationTemplateListResponse> response =
        applicationTemplateFixture.create(workspace.hrToken(), workspace.workspaceId(), request);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().name()).isEqualTo("backend-default-template");
    assertThat(response.getData().used()).isTrue();
    assertThat(response.getData().recruitmentCount()).isZero();
    assertThat(response.getData().customFields()).contains("portfolioUrl");
  }

  @Test
  void 비인사담당자는_지원서_템플릿을_생성할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    // Arrange
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    userFixture.signUp(interviewerEmail, interviewerPassword, userFixture.randomName());
    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(
            workspace.hrToken(), workspace.workspaceId(), interviewerEmail, MemberType.INTERVIEWER);
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();
    memberFixture.acceptInvitation(invitationResponse.getData().token(), interviewerToken);
    Map<String, Object> request =
        applicationTemplateFixture.newCreateRequest(
            "interviewer-template",
            List.of(Map.of("key", "githubUrl", "label", "GitHub URL", "type", "TEXT")),
            false);

    // Act
    ApiResponse<ApplicationTemplateListResponse> response =
        applicationTemplateFixture.create(interviewerToken, workspace.workspaceId(), request);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getCode()).isEqualTo("E403");
  }
}
