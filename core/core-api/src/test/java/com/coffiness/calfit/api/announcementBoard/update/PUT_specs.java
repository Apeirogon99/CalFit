package com.coffiness.calfit.api.announcementBoard.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("PUT /api/v1/announcement-boards/{boardId}")
class PUT_specs {

  @Test
  void 올바른_요청_시_200_OK를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().pinned()).isTrue();
  }

  @Test
  void 토큰이_없으면_401을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(null, tenantId, boardId, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_boardId이면_에러를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, 99999L, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 제목이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, null, "내용 B", true);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 제목이_한글자면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "공", "내용 B", true);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 제목이_100자를_초과하면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "가".repeat(101), "내용 B", true);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 내용이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "공지 B", null, true);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void pinned_값이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "공지 B", "내용 B", null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인사_담당자가_아니면_수정할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long memberId = memberFixture.getMyMember(token, tenantId).getData().id();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);
    memberFixture.updateMember(
        memberId, new UpdateMemberRequest(MemberType.INTERVIEWER), token, tenantId);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, tenantId, boardId, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_공지사항과_같은_제목으로도_수정할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String duplicatedTitle = "같은 제목";

    createBoardAndGetId(announcementBoardFixture, token, tenantId, duplicatedTitle, "내용 A", false);
    long targetBoardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "다른 제목", "내용 B", true);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(
            token, tenantId, targetBoardId, duplicatedTitle, "수정된 내용", false);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isEqualTo(targetBoardId);
    assertThat(response.getData().title()).isEqualTo(duplicatedTitle);
  }

  @Test
  void 수정하면_상세_조회에도_변경사항이_반영된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "수정 전", "수정 전 내용", false);

    announcementBoardFixture.update(token, tenantId, boardId, "수정 후", "수정 후 내용", true);
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().title()).isEqualTo("수정 후");
    assertThat(response.getData().content()).isEqualTo("수정 후 내용");
    assertThat(response.getData().pinned()).isTrue();
  }

  private long createBoardAndGetId(
      AnnouncementBoardFixture announcementBoardFixture,
      String token,
      String tenantId,
      String title,
      String content,
      Boolean pinned) {
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, tenantId, title, content, pinned);
    return response.getData().id();
  }
}
