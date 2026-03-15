package com.coffiness.calfit.api.notification.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("PATCH /api/v1/notifications")
class PATCH_specs {

  @Test
  void 알림을_읽음처리하고_전체읽음처리할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 1", "내용 1", false);
    announcementBoardFixture.create(token, tenantId, "공지 2", "내용 2", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                    .isGreaterThanOrEqualTo(2));

    NotificationResponse firstNotification =
        notificationFixture.unread(token, tenantId, null, 0, 10).getData().contents().get(0);

    ApiResponse<NotificationResponse> readResponse =
        notificationFixture.read(token, tenantId, firstNotification.id());

    assertThat(readResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(readResponse.getData().isRead()).isTrue();

    notificationFixture.readAll(token, tenantId);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                    .isZero());
  }
}
