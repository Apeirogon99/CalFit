package com.coffiness.calfit.api.calendar.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/schedules/sync")
public class POST_specs {

  @Test
  void 올바른_요청으로_동기화하면_성공_응답을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    ScheduleSyncRequest request =
        new ScheduleSyncRequest(
            "google-event-1",
            "동기화 일정",
            "구글 캘린더에서 내려온 일정",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false);

    // Act
    ApiResponse<Void> response = calendarFixture.syncSchedule(token, tenantId, request);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 같은_googleEventId로_재동기화하면_일정은_한건만_유지된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    String googleEventId = "google-event-duplicate";

    ScheduleSyncRequest firstRequest =
        new ScheduleSyncRequest(
            googleEventId,
            "초기 동기화 일정",
            "최초 동기화",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false);

    ScheduleSyncRequest secondRequest =
        new ScheduleSyncRequest(
            googleEventId,
            "수정 동기화 일정",
            "재동기화",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(1),
            false);

    // Act
    ApiResponse<Void> firstResponse = calendarFixture.syncSchedule(token, tenantId, firstRequest);
    ApiResponse<Void> secondResponse = calendarFixture.syncSchedule(token, tenantId, secondRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    var schedules = calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();

    // Assert
    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules).hasSize(1);
    assertThat(schedules.get(0).title()).isEqualTo("수정 동기화 일정");
  }
}
