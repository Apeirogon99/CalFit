package com.coffiness.calfit.api.member.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("DELETE /api/v1/members/{memberId}")
public class DELETE_specs {

  @Test
  void 정상_삭제_요청_시_204를_반환하고_해당_멤버는_목록에서_제거된다(@Autowired MemberFixture fixture) {
    // Arrange: 두 번째 멤버 추가
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act
    ApiResponse<Void> deleteResponse =
        fixture.removeMember(secondMemberId, ctx.hrToken(), ctx.workspaceId());

    // Assert: 삭제 성공
    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Assert: 멤버 목록에서 제거됨
    ApiResponse<MemberResponse[]> membersResponse =
        fixture.getMembers(ctx.hrToken(), ctx.workspaceId());
    boolean stillExists =
        Arrays.stream(membersResponse.getData()).anyMatch(m -> m.id().equals(secondMemberId));
    assertThat(stillExists).isFalse();
  }

  @Test
  void 존재하지_않는_memberId이면_404_Not_Found를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long nonExistentMemberId = Long.MAX_VALUE;

    // Act
    ApiResponse<Void> response =
        fixture.removeMember(nonExistentMemberId, ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 권한_없는_사용자가_요청하면_403_Forbidden을_반환한다(@Autowired MemberFixture fixture) {
    // TODO: 멤버 역할 기반 권한 체크 로직 구현 후 작성
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act
    ApiResponse<Void> response = fixture.removeMember(secondMemberId, null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 자기_자신을_삭제하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // TODO: MemberService.removeMember에 자기 자신 삭제 방지 로직 추가 후 작성
    // 구현 방향: removeMember(memberId, requestUserId) 시그니처로 변경하여
    //           삭제 대상 멤버의 memberId == requestUserId이면 CoreException 발생
  }

  @Test
  void 다른_워크스페이스의_멤버를_삭제하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: 워크스페이스 A, B 각각 생성
    WorkspaceContext ctxA = fixture.setupWorkspace();
    WorkspaceContext ctxB = fixture.setupWorkspace();
    Long memberBId = fixture.addSecondMember(ctxB);

    // Act: A의 토큰과 테넌트로 B 멤버를 삭제 시도
    ApiResponse<Void> response =
        fixture.removeMember(memberBId, ctxA.hrToken(), ctxA.workspaceId());

    // Assert: Hibernate @TenantId 필터링으로 NOT_FOUND 반환
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
