package com.coffiness.calfit.api.member.me;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/members/me")
public class GET_specs {

  @Test
  void 올바르게_요청하면_내_멤버_정보를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse> response = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse> response = fixture.getMyMember(null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 워크스페이스_생성자는_HR_역할로_등록된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse> response = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getData().memberType()).isEqualTo(MemberType.HR);
  }

  @Test
  void 반환된_멤버_정보에_id와_name이_포함된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse> response = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId());
    MemberResponse member = response.getData();

    // Assert
    assertThat(member.id()).isNotNull();
    assertThat(member.name()).isNotBlank();
  }

  @Test
  void group에_배정된_내_멤버_정보는_groupId를_포함한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();
    GroupResponse group =
        fixture.createGroup("인사팀", "#10B981", ctx.hrToken(), ctx.workspaceId()).getData();
    fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Act
    ApiResponse<MemberResponse> response = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getData().groupId()).isEqualTo(group.id());
    assertThat(response.getData().group()).isEqualTo(group.name());
  }
}
