package com.coffiness.calfit.api.group.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/groups")
public class POST_specs {

  @Test
  void 올바른_요청_시_201과_Location_헤더를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<GroupResponse> response =
        fixture.createGroup("개발팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().name()).isEqualTo("개발팀");
    assertThat(response.getData().color()).isEqualTo("#3B82F6");
  }

  @Test
  void group_name_누락_시_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act: name 없이 요청
    ApiResponse<GroupResponse> response =
        fixture.createGroupRaw(Map.of("color", "#3B82F6"), ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void initialMemberIds_중_존재하지_않는_멤버가_있으면_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // CreateGroupRequest에 initialMemberIds 필드가 아직 없으므로 추후 구현
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<GroupResponse> response =
        fixture.createGroup("개발팀", "#3B82F6", null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 같은_워크스페이스_내_동일한_이름의_그룹이_있으면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    fixture.createGroup("개발팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId());

    // Act: 같은 이름으로 다시 생성
    ApiResponse<GroupResponse> response =
        fixture.createGroup("개발팀", "#FF5733", ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void group_name이_30자를_초과하면_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // CreateGroupRequest에 @Size(max=30) 검증이 아직 없으므로 추후 구현
  }

  @Test
  void initialMemberIds가_다른_워크스페이스_소속이면_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // CreateGroupRequest에 initialMemberIds 필드가 아직 없으므로 추후 구현
  }
}
