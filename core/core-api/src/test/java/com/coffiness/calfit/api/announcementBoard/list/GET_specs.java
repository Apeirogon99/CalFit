package com.coffiness.calfit.api.announcementBoard.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardListResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/announcement-boards")
class GET_specs {

  @Test
  void 공지사항이_없으면_빈_목록을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }

  @Test
  void 정상_요청_시_200_OK와_공지사항_목록을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);

    // Act
    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 생성한_공지사항의_제목과_내용을_목록에서_확인할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    assertThat(response.getData())
        .anySatisfy(
            announcement -> {
              assertThat(announcement.title()).isEqualTo("공지 제목");
              assertThat(announcement.content()).isEqualTo("공지 내용");
              assertThat(announcement.createdAt()).isNotNull();
            });
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Act
    ApiResponse<AnnouncementBoardListResponse[]> response = announcementBoardFixture.list();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_워크스페이스의_공지사항은_목록에_보이지_않는다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext firstContext = memberFixture.setupWorkspace();
    MemberFixture.WorkspaceContext secondContext = memberFixture.setupWorkspace();

    announcementBoardFixture.create(
        firstContext.hrToken(), firstContext.workspaceId(), "첫 워크스페이스 공지", "내용", true);

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(secondContext.hrToken(), secondContext.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData())
        .extracting(AnnouncementBoardListResponse::title)
        .doesNotContain("첫 워크스페이스 공지");
  }

  @Test
  void 공지사항_목록이_pinned_우선으로_정렬된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    announcementBoardFixture.create(token, tenantId, "일반 공지", "일반 내용", false);
    announcementBoardFixture.create(token, tenantId, "고정 공지", "고정 내용", true);

    // Act
    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isGreaterThanOrEqualTo(2);
    assertThat(response.getData()[0].pinned()).isTrue();
  }

  @Test
  void pinned_상태가_같으면_최신_공지사항이_먼저_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    announcementBoardFixture.create(token, tenantId, "이전 공지", "이전 내용", false);
    announcementBoardFixture.create(token, tenantId, "최신 공지", "최신 내용", false);

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()[0].title()).isEqualTo("최신 공지");
  }

  @Test
  void 고정_공지사항이_여러개면_모두_일반_공지보다_앞에_온다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    announcementBoardFixture.create(token, tenantId, "일반 공지", "일반 내용", false);
    announcementBoardFixture.create(token, tenantId, "고정 공지 1", "고정 내용 1", true);
    announcementBoardFixture.create(token, tenantId, "고정 공지 2", "고정 내용 2", true);

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSizeGreaterThanOrEqualTo(3);
    assertThat(response.getData()[0].pinned()).isTrue();
    assertThat(response.getData()[1].pinned()).isTrue();
  }

  @Test
  void 같은_제목의_공지사항도_목록에_각각_노출된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "중복 제목", "첫 번째", false);
    announcementBoardFixture.create(token, tenantId, "중복 제목", "두 번째", false);

    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    assertThat(response.getData())
        .filteredOn(announcement -> announcement.title().equals("중복 제목"))
        .hasSize(2);
  }

  @Test
  void 삭제된_공지사항은_목록에서_제외된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long boardId =
        announcementBoardFixture.create(token, tenantId, "삭제할 공지", "내용", false).getData().id();

    announcementBoardFixture.delete(token, tenantId, boardId);
    ApiResponse<AnnouncementBoardListResponse[]> response =
        announcementBoardFixture.list(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData())
        .extracting(AnnouncementBoardListResponse::title)
        .doesNotContain("삭제할 공지");
  }
}
