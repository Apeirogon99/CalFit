package com.coffiness.calfit.api.workspace.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces")
public class POST_specs {

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response = fixture.createWorkspace(token);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized_상태코드를_반환한다(@Autowired WorkspaceFixture fixture) {
    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(null, "스타트업허브", "010-1234-5678", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_지정되지_않으면_400_Bad_Request를_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, null, "010-1234-5678", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_2자_미만이면_400_Bad_Request를_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, "A", "010-1234-5678", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_50자_초과이면_400_Bad_Request를_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();
    String longName = "A".repeat(51);

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, longName, "010-1234-5678", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void contactPhone_속성이_없으면_400_Bad_Request를_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, "스타트업허브", null, "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void contactPhone_형식이_올바르지_않으면_400_Bad_Request를_반환한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, "스타트업허브", "invalid-phone", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 워크스페이스_정보를_올바르게_생성한다(@Autowired WorkspaceFixture fixture) {
    String token = fixture.createUserAndGetToken();

    ApiResponse<WorkspaceResponse> response =
        fixture.createWorkspace(token, "스타트업허브", "010-1234-5678", "1-10");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().workspaceId()).isNotBlank();
    assertThat(response.getData().name()).isEqualTo("스타트업허브");
    assertThat(response.getData().contactPhone()).isEqualTo("010-1234-5678");
    assertThat(response.getData().employeeScale()).isEqualTo("1-10");
  }
}
