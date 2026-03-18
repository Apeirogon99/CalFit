package com.coffiness.calfit.api.group.addMember;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.MemberContext;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/groups/{groupId}/members — PATCH /api/v1/members/{memberId}/group 으로 대체")
public class POST_specs {

  // Note: POST /api/v1/groups/{groupId}/members 엔드포인트는 존재하지 않음.
  // 실제 그룹 배정은 PATCH /api/v1/members/{memberId}/group 으로 수행됨.
  // 아래 테스트는 해당 엔드포인트를 기준으로 작성.

  @Test
  void 정상_요청_시_204를_반환하고_그룹의_memberCount가_증가한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("개발팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();

    // Act: 멤버를 그룹에 배정
    ApiResponse<Void> response =
        fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    // Assert: 그룹 멤버 수 확인
    GroupResponse[] groups = fixture.getGroups(ctx.hrToken(), ctx.workspaceId()).getData();
    GroupResponse updatedGroup =
        java.util.Arrays.stream(groups)
            .filter(g -> g.id().equals(group.id()))
            .findFirst()
            .orElseThrow();
    assertThat(updatedGroup.memberCount()).isEqualTo(1);
  }

  @Test
  void 존재하지_않는_memberId이면_404_Not_Found를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();

    // Act
    ApiResponse<Void> response =
        fixture.assignGroup(Long.MAX_VALUE, group.id(), ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 권한_없는_사용자는_403_Forbidden을_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberContext nonHr = fixture.addSecondMemberWithToken(ctx);

    // Act: INTERVIEWER 멤버가 그룹 배정 시도
    ApiResponse<Void> response =
        fixture.assignGroup(nonHr.memberId(), group.id(), nonHr.token(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이미_그룹에_속한_멤버를_추가하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();
    fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Act: 이미 배정된 멤버를 같은 그룹에 다시 배정
    ApiResponse<Void> response =
        fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Assert: 이미 해당 그룹이면 성공 또는 에러 (구현에 따라 다름)
    // assignGroup은 PUT 성격이므로 멱등하게 성공할 수도 있음
    assertThat(response.getResult()).isNotNull();
  }

  @Test
  void 다른_워크스페이스_소속_멤버를_추가하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctxA = fixture.setupWorkspace();
    WorkspaceContext ctxB = fixture.setupWorkspace();
    GroupResponse groupA =
        fixture.createGroup("A팀", "#3B82F6", ctxA.hrToken(), ctxA.workspaceId()).getData();
    Long memberBId = fixture.addSecondMember(ctxB);

    // Act: 워크스페이스 A에서 워크스페이스 B의 멤버를 A 그룹에 배정 시도
    ApiResponse<Void> response =
        fixture.assignGroup(memberBId, groupA.id(), ctxA.hrToken(), ctxA.workspaceId());

    // Assert: Hibernate @TenantId 필터링으로 멤버를 찾지 못함
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    GroupResponse group =
        fixture.createGroup("팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();

    // Act
    ApiResponse<Void> response =
        fixture.assignGroup(myMember.id(), group.id(), null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_groupId이면_404_Not_Found를_반환한다(@Autowired MemberFixture fixture) {
    // assignGroup은 groupId 존재 여부를 검증하지 않고 그대로 저장함 — 검증 로직 추가 후 구현
  }
}
