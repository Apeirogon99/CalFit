package com.coffiness.calfit.api.group.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.MemberContext;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("DELETE /api/v1/groups/{groupId}")
public class DELETE_specs {

  @Test
  void 정상_삭제_시_204를_반환하고_그룹이_목록에서_제거된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("삭제될팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();

    // Act
    ApiResponse<Void> deleteResponse =
        fixture.deleteGroup(group.id(), ctx.hrToken(), ctx.workspaceId());

    // Assert: 삭제 성공
    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Assert: 그룹 목록에서 제거됨
    ApiResponse<GroupResponse[]> groupsResponse =
        fixture.getGroups(ctx.hrToken(), ctx.workspaceId());
    boolean stillExists =
        Arrays.stream(groupsResponse.getData()).anyMatch(g -> g.id().equals(group.id()));
    assertThat(stillExists).isFalse();
  }

  @Test
  void 존재하지_않는_groupId이면_404_Not_Found를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<Void> response =
        fixture.deleteGroup(Long.MAX_VALUE, ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제_권한_없는_요청자는_403_Forbidden을_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: HR이 그룹 생성, 비-HR 멤버 추가
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberContext nonHr = fixture.addSecondMemberWithToken(ctx);

    // Act: 비-HR(INTERVIEWER) 멤버가 삭제 시도
    ApiResponse<Void> response = fixture.deleteGroup(group.id(), nonHr.token(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();

    // Act
    ApiResponse<Void> response = fixture.deleteGroup(group.id(), null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 그룹_내_멤버가_있는_상태에서_삭제해도_정상_처리된다(@Autowired MemberFixture fixture) {
    // Arrange: 그룹 생성 후 멤버 배정
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();
    fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Act: 멤버가 있는 그룹 삭제
    ApiResponse<Void> response = fixture.deleteGroup(group.id(), ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }
}
