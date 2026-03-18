package com.coffiness.calfit.api.announcementBoard.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/announcement-boards/{id}")
class GET_specs {

  @Test
  void 공지사항_상세_조회가_성공하고_createdAt을_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<AnnouncementBoardResponse> createResponse =
        announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", true);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, createResponse.getData().id());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isEqualTo(createResponse.getData().id());
    assertThat(response.getData().title()).isEqualTo("공지 제목");
    assertThat(response.getData().content()).isEqualTo("공지 내용");
    assertThat(response.getData().pinned()).isTrue();
    assertThat(response.getData().createdAt()).isNotNull();
  }

  @Test
  void 고정되지_않은_공지사항도_상세_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long boardId =
        announcementBoardFixture.create(token, tenantId, "일반 공지", "일반 내용", false).getData().id();

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().pinned()).isFalse();
  }

  @Test
  void 존재하지_않는_공지사항_ID로_조회하면_에러를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(context.hrToken(), context.workspaceId(), 99999L);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_워크스페이스의_공지사항은_조회할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext firstContext = memberFixture.setupWorkspace();
    MemberFixture.WorkspaceContext secondContext = memberFixture.setupWorkspace();

    Long boardId =
        announcementBoardFixture
            .create(firstContext.hrToken(), firstContext.workspaceId(), "다른 테넌트 공지", "내용", true)
            .getData()
            .id();

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(
            secondContext.hrToken(), secondContext.workspaceId(), boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제된_공지사항은_상세_조회할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long boardId =
        announcementBoardFixture.create(token, tenantId, "삭제 예정 공지", "내용", false).getData().id();
    announcementBoardFixture.delete(token, tenantId, boardId);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 수정한_공지사항은_상세_조회에_반영된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long boardId =
        announcementBoardFixture
            .create(token, tenantId, "수정 전 제목", "수정 전 내용", false)
            .getData()
            .id();
    announcementBoardFixture.update(token, tenantId, boardId, "수정 후 제목", "수정 후 내용", true);

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().title()).isEqualTo("수정 후 제목");
    assertThat(response.getData().content()).isEqualTo("수정 후 내용");
    assertThat(response.getData().pinned()).isTrue();
  }

  @Test
  void 연속_생성한_공지사항은_각각_다른_ID로_상세_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long firstId =
        announcementBoardFixture.create(token, tenantId, "첫 공지", "첫 내용", false).getData().id();
    Long secondId =
        announcementBoardFixture.create(token, tenantId, "둘 공지", "둘 내용", false).getData().id();

    ApiResponse<AnnouncementBoardResponse> firstResponse =
        announcementBoardFixture.detail(token, tenantId, firstId);
    ApiResponse<AnnouncementBoardResponse> secondResponse =
        announcementBoardFixture.detail(token, tenantId, secondId);

    assertThat(firstResponse.getData().id()).isNotEqualTo(secondResponse.getData().id());
    assertThat(firstResponse.getData().title()).isEqualTo("첫 공지");
    assertThat(secondResponse.getData().title()).isEqualTo("둘 공지");
  }

  @Test
  void 상세_조회_응답은_생성시_입력한_내용을_보존한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String content = "줄바꿈 없이도 충분히 긴 공지 내용입니다.";

    Long boardId =
        announcementBoardFixture.create(token, tenantId, "내용 보존 공지", content, false).getData().id();

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().content()).isEqualTo(content);
  }

  @Test
  void pinned_값은_상세_조회에서_정확히_반환된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long pinnedBoardId =
        announcementBoardFixture.create(token, tenantId, "고정 공지", "내용", true).getData().id();
    Long normalBoardId =
        announcementBoardFixture.create(token, tenantId, "일반 공지", "내용", false).getData().id();

    ApiResponse<AnnouncementBoardResponse> pinnedResponse =
        announcementBoardFixture.detail(token, tenantId, pinnedBoardId);
    ApiResponse<AnnouncementBoardResponse> normalResponse =
        announcementBoardFixture.detail(token, tenantId, normalBoardId);

    assertThat(pinnedResponse.getData().pinned()).isTrue();
    assertThat(normalResponse.getData().pinned()).isFalse();
  }

  @Test
  void 상세_조회시_createdAt은_항상_존재한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long boardId =
        announcementBoardFixture.create(token, tenantId, "생성시각 공지", "내용", false).getData().id();

    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.detail(token, tenantId, boardId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().createdAt()).isNotNull();
  }
}
