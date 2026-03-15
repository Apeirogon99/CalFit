package com.coffiness.calfit.api.notification.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture.NotificationPageResponse;
import com.coffiness.calfit.api.v1.response.NotificationUnreadCountResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/notifications")
class GET_specs {

  @Test
  void 공지사항_생성시_알림_목록과_미확인_개수가_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              ApiResponse<NotificationUnreadCountResponse> unreadCountResponse =
                  notificationFixture.unreadCount(token, tenantId);
              assertThat(unreadCountResponse.getResult()).isEqualTo(ResultType.SUCCESS);
              assertThat(unreadCountResponse.getData().unreadCount()).isGreaterThanOrEqualTo(1);
            });

    ApiResponse<NotificationPageResponse> unreadResponse =
        notificationFixture.unread(token, tenantId, "ANNOUNCEMENT", 0, 5);

    assertThat(unreadResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(unreadResponse.getData().contents()).isNotEmpty();
    assertThat(unreadResponse.getData().contents().get(0).type().name())
        .isEqualTo("ANNOUNCEMENT_CREATED");
  }
}
