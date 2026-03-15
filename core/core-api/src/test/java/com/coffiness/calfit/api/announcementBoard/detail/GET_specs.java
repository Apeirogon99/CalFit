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
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
}
