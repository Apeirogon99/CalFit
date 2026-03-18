package com.coffiness.calfit.api.group.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/groups")
public class GET_specs {

  @Test
  void 정상_요청_시_200_OK와_그룹_리스트를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    fixture.createGroup("개발팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId());

    // Act
    ApiResponse<GroupResponse[]> response = fixture.getGroups(ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotEmpty();
  }

  @Test
  void 비인가_사용자면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<GroupResponse[]> response = fixture.getGroups(null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 그룹_정보의_이름_색상_멤버수가_정확히_반환된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    fixture.createGroup("디자인팀", "#FF5733", ctx.hrToken(), ctx.workspaceId());

    // Act
    ApiResponse<GroupResponse[]> response = fixture.getGroups(ctx.hrToken(), ctx.workspaceId());
    GroupResponse group = response.getData()[0];

    // Assert
    assertThat(group.name()).isEqualTo("디자인팀");
    assertThat(group.color()).isEqualTo("#FF5733");
    assertThat(group.memberCount()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void 다른_워크스페이스의_그룹은_포함되지_않는다(@Autowired MemberFixture fixture) {
    // Arrange: 워크스페이스 A, B 각각 그룹 생성
    WorkspaceContext ctxA = fixture.setupWorkspace();
    WorkspaceContext ctxB = fixture.setupWorkspace();
    fixture.createGroup("A팀", "#3B82F6", ctxA.hrToken(), ctxA.workspaceId());
    fixture.createGroup("B팀", "#FF5733", ctxB.hrToken(), ctxB.workspaceId());

    // Act: 워크스페이스 A 기준으로 그룹 조회
    ApiResponse<GroupResponse[]> responseA = fixture.getGroups(ctxA.hrToken(), ctxA.workspaceId());

    // Assert: A팀만 있어야 하고, B팀은 포함되면 안 됨
    assertThat(responseA.getData()).hasSize(1);
    assertThat(responseA.getData()[0].name()).isEqualTo("A팀");
  }

  @Test
  void 그룹이_없으면_빈_리스트를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: 존재하지 않는 워크스페이스를 테넌트로 사용
    String token = fixture.userFixture().createUserAndGetToken();
    String nonExistentTenantId = UUID.randomUUID().toString();

    // Act
    ApiResponse<GroupResponse[]> response = fixture.getGroups(token, nonExistentTenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }
}
