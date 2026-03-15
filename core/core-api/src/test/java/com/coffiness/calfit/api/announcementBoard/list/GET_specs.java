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
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/announcement-boards")
class GET_specs {

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
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Act
    ApiResponse<AnnouncementBoardListResponse[]> response = announcementBoardFixture.list();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
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
}
