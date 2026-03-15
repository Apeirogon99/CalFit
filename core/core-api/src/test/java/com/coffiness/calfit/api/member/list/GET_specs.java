package com.coffiness.calfit.api.member.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/members")
public class GET_specs {

  @Test
  void 올바른_토큰이면_200_OK와_멤버_목록을_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse[]> response = fixture.getMembers(ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse[]> response = fixture.getMembers(null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_워크스페이스의_멤버가_포함되어서는_안_된다(@Autowired MemberFixture fixture) {
    // Arrange: 워크스페이스 A, B 각각 생성
    WorkspaceContext ctxA = fixture.setupWorkspace();
    WorkspaceContext ctxB = fixture.setupWorkspace();
    // 워크스페이스 B에 두 번째 멤버 추가
    fixture.addSecondMember(ctxB);

    // Act: 워크스페이스 A 기준으로 멤버 조회
    ApiResponse<MemberResponse[]> responseA =
        fixture.getMembers(ctxA.hrToken(), ctxA.workspaceId());

    // Assert: A는 자기 자신(1명)만 있어야 함 — B의 멤버가 포함되면 안 됨
    assertThat(responseA.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(responseA.getData()).hasSize(1);
  }

  @Test
  void 각_MemberView의_id_name_role이_올바르게_반환된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();

    // Act
    ApiResponse<MemberResponse[]> response = fixture.getMembers(ctx.hrToken(), ctx.workspaceId());
    MemberResponse member = response.getData()[0];

    // Assert
    assertThat(member.id()).isNotNull();
    assertThat(member.name()).isNotBlank();
    assertThat(member.memberType()).isEqualTo(MemberType.HR);
  }

  @Test
  void group에_배정된_멤버는_groupId와_group명이_함께_반환된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    MemberResponse myMember = fixture.getMyMember(ctx.hrToken(), ctx.workspaceId()).getData();
    GroupResponse group =
        fixture.createGroup("개발팀", "#3B82F6", ctx.hrToken(), ctx.workspaceId()).getData();
    fixture.assignGroup(myMember.id(), group.id(), ctx.hrToken(), ctx.workspaceId());

    // Act
    ApiResponse<MemberResponse[]> response = fixture.getMembers(ctx.hrToken(), ctx.workspaceId());
    MemberResponse member =
        java.util.Arrays.stream(response.getData())
            .filter(item -> item.id().equals(myMember.id()))
            .findFirst()
            .orElseThrow();

    // Assert
    assertThat(member.groupId()).isEqualTo(group.id());
    assertThat(member.group()).isEqualTo(group.name());
  }

  @Test
  void 멤버_목록은_최신순으로_정렬된다(@Autowired MemberFixture fixture) {
    // TODO: MemberService.getMembers에 정렬 로직(createdAt DESC) 추가 후 구현
  }

  @Test
  void 멤버가_없으면_빈_리스트를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: 존재하지 않는 워크스페이스 ID를 tenant로 사용
    String token = fixture.userFixture().createUserAndGetToken();
    String nonExistentTenantId = UUID.randomUUID().toString();

    // Act
    ApiResponse<MemberResponse[]> response = fixture.getMembers(token, nonExistentTenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }
}
