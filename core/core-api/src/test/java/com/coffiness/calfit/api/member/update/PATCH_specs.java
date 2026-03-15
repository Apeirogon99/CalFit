package com.coffiness.calfit.api.member.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("PATCH /api/v1/members/{memberId}")
public class PATCH_specs {

  @Test
  void 정상_요청_시_204를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: 두 번째 멤버 추가 후 그 멤버의 ID 획득
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act
    ApiResponse<Void> response =
        fixture.updateMember(
            secondMemberId,
            new UpdateMemberRequest(MemberType.HR),
            ctx.hrToken(),
            ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 유효하지_않은_role이면_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act: 존재하지 않는 MemberType 문자열 전송 → JSON 역직렬화 실패
    ApiResponse<Void> response =
        fixture.updateMember(
            secondMemberId, Map.of("memberType", "INVALID_ROLE"), ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_groupId_지정_시_에러를_반환한다(@Autowired MemberFixture fixture) {
    // TODO: Group 도메인 구현 후 작성
  }

  @Test
  void 권한_없는_사용자가_요청하면_403_Forbidden을_반환한다(@Autowired MemberFixture fixture) {
    // TODO: 멤버 역할 기반 권한 체크 로직 구현 후 작성
  }

  @Test
  void 존재하지_않는_memberId이면_404_Not_Found를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long nonExistentMemberId = Long.MAX_VALUE;

    // Act
    ApiResponse<Void> response =
        fixture.updateMember(
            nonExistentMemberId,
            new UpdateMemberRequest(MemberType.INTERVIEWER),
            ctx.hrToken(),
            ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void role만_변경해도_정상_처리된다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act: MEMBER → HR 변경
    ApiResponse<Void> response =
        fixture.updateMember(
            secondMemberId,
            new UpdateMemberRequest(MemberType.HR),
            ctx.hrToken(),
            ctx.workspaceId());

    // Assert: 변경 성공 확인
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    // Assert: 실제 값이 변경됐는지 멤버 목록으로 확인
    ApiResponse<MemberResponse[]> members = fixture.getMembers(ctx.hrToken(), ctx.workspaceId());
    boolean updated =
        java.util.Arrays.stream(members.getData())
            .anyMatch(m -> m.id().equals(secondMemberId) && m.memberType() == MemberType.HR);
    assertThat(updated).isTrue();
  }

  @Test
  void groupId만_변경해도_정상_처리된다(@Autowired MemberFixture fixture) {
    // TODO: Group 도메인 구현 후 작성
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act
    ApiResponse<Void> response =
        fixture.updateMember(
            secondMemberId, new UpdateMemberRequest(MemberType.HR), null, ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_워크스페이스의_멤버를_수정하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange: 워크스페이스 A, B 각각 생성
    WorkspaceContext ctxA = fixture.setupWorkspace();
    WorkspaceContext ctxB = fixture.setupWorkspace();
    Long memberBId = fixture.addSecondMember(ctxB);

    // Act: A의 토큰과 테넌트로 B 멤버를 수정 시도
    ApiResponse<Void> response =
        fixture.updateMember(
            memberBId, new UpdateMemberRequest(MemberType.HR), ctxA.hrToken(), ctxA.workspaceId());

    // Assert: Hibernate @TenantId 필터링으로 NOT_FOUND 반환
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 요청_본문이_비어있으면_400_Bad_Request를_반환한다(@Autowired MemberFixture fixture) {
    // Arrange
    WorkspaceContext ctx = fixture.setupWorkspace();
    Long secondMemberId = fixture.addSecondMember(ctx);

    // Act: body null 전송
    ApiResponse<Void> response =
        fixture.updateMember(secondMemberId, null, ctx.hrToken(), ctx.workspaceId());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
