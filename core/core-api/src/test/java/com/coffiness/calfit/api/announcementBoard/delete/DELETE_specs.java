package com.coffiness.calfit.api.announcementBoard.delete;

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
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DELETE /api/v1/announcement-boards/{boardId}")
class DELETE_specs {

  @Test
  void 정상_삭제_시_성공을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", true);

    // Act
    ApiResponse<Void> response = announcementBoardFixture.delete(token, tenantId, boardId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 토큰을_제공하지_않으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", false);

    // Act
    ApiResponse<Void> response = announcementBoardFixture.delete(null, tenantId, boardId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인사_담당자가_아니면_삭제할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, "공지 A", "내용 A", true);
    Long memberId = memberFixture.getMyMember(token, tenantId).getData().id();
    memberFixture.updateMember(
        memberId, new UpdateMemberRequest(MemberType.INTERVIEWER), token, tenantId);

    // Act
    ApiResponse<Void> response = announcementBoardFixture.delete(token, tenantId, boardId);

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
    ApiResponse<Void> response = announcementBoardFixture.delete(token, tenantId, 99999L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제한_공지사항과_같은_제목으로_다시_생성할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String title = "재사용 제목";

    long boardId =
        createBoardAndGetId(announcementBoardFixture, token, tenantId, title, "내용 A", false);

    ApiResponse<Void> deleteResponse = announcementBoardFixture.delete(token, tenantId, boardId);
    ApiResponse<AnnouncementBoardResponse> recreateResponse =
        announcementBoardFixture.create(token, tenantId, title, "내용 B", true);

    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreateResponse.getData()).isNotNull();
    assertThat(recreateResponse.getData().id()).isNotEqualTo(boardId);
    assertThat(recreateResponse.getData().title()).isEqualTo(title);
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
