package com.coffiness.calfit.api.announcementBoard.create;

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
@DisplayName("POST /api/v1/announcement-boards")
class POST_specs {

  @Test
  void 정상_요청시_공지사항이_생성된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
    assertThat(response.getData().pinned()).isTrue();
  }

  @Test
  void 인증_토큰이_없으면_401을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(null, context.workspaceId(), "공지 제목", "공지 내용", false);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 제목이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, tenantId, null, "공지 내용", false);

    // Assert
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

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인사_담당자가_아니면_생성할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long memberId = memberFixture.getMyMember(token, tenantId).getData().id();
    memberFixture.updateMember(
        memberId, new UpdateMemberRequest(MemberType.INTERVIEWER), token, tenantId);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 같은_제목의_공지사항도_중복_생성할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String title = "중복 허용 제목";

    ApiResponse<AnnouncementBoardResponse> firstResponse =
        announcementBoardFixture.create(token, tenantId, title, "첫 번째 내용", false);
    ApiResponse<AnnouncementBoardResponse> secondResponse =
        announcementBoardFixture.create(token, tenantId, title, "두 번째 내용", true);

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getData()).isNotNull();
    assertThat(secondResponse.getData().id()).isNotEqualTo(firstResponse.getData().id());
    assertThat(secondResponse.getData().title()).isEqualTo(title);
  }
}
