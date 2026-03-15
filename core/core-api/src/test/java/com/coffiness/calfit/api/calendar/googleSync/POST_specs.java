package com.coffiness.calfit.api.calendar.googleSync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/calendars/google/sync")
public class POST_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 구글_동기화를_요청하면_구글_일정이_캘핏_일정으로_생성된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    connect(calendarFixture, token, tenantId, "google-sync-auth-code-1");

    ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1);
    ZonedDateTime endTime = startTime.plusHours(1);

    given(googleCalendarPort.syncEvent(anyString(), anyString(), any()))
        .willReturn(
            new GoogleCalendarSyncResult(
                List.of(
                    new GoogleCalendarSyncResult.SyncEventModel(
                        "google-sync-event-1",
                        "confirmed",
                        "구글에서 내려온 일정",
                        "구글 동기화 설명",
                        startTime,
                        endTime,
                        false,
                        false)),
                "next-sync-token-1"));

    // Act
    ApiResponse<Void> response = calendarFixture.syncGoogleCalendar(token, tenantId);

    String startDate = LocalDateTime.now().toLocalDate().toString();
    String endDate = LocalDateTime.now().toLocalDate().plusDays(3).toString();
    var schedules = calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules).hasSize(1);
    assertThat(schedules.get(0).title()).isEqualTo("구글에서 내려온 일정");
  }

  @Test
  void 구글에서_삭제된_일정을_동기화하면_캘핏_일정도_삭제된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    connect(calendarFixture, token, tenantId, "google-sync-auth-code-2");

    LocalDateTime now = LocalDateTime.now();
    ScheduleSyncRequest request =
        new ScheduleSyncRequest(
            "google-delete-event-1",
            "삭제 대상 일정",
            "구글 취소 이벤트 테스트",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false);

    calendarFixture.syncSchedule(token, tenantId, request);

    ZonedDateTime startTime = now.plusDays(1).atZone(ZoneId.systemDefault());
    ZonedDateTime endTime = now.plusDays(1).plusHours(1).atZone(ZoneId.systemDefault());

    given(googleCalendarPort.syncEvent(anyString(), anyString(), any()))
        .willReturn(
            new GoogleCalendarSyncResult(
                List.of(
                    new GoogleCalendarSyncResult.SyncEventModel(
                        "google-delete-event-1",
                        "cancelled",
                        "삭제 대상 일정",
                        "구글 취소 이벤트 테스트",
                        startTime,
                        endTime,
                        false,
                        false)),
                "next-sync-token-2"));

    // Act
    ApiResponse<Void> response = calendarFixture.syncGoogleCalendar(token, tenantId);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    var schedules = calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules).isEmpty();
  }

  private void connect(
      CalendarFixture calendarFixture, String token, String tenantId, String authCode) {
    given(
            googleOAuthPort.exchangeAuthorizationCode(
                authCode, "http://localhost:5173/auth/callback"))
        .willReturn(
            new OAuthExchangeResult(
                "access-token", 3600L, "refresh-token", createIdToken("sync@test.com")));

    given(googleCalendarPort.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .willReturn("workspace-calendar-id");

    ApiResponse<String> connectResponse =
        calendarFixture.connectGoogleCalendar(token, tenantId, authCode);

    assertThat(connectResponse.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  private String createIdToken(String email) {
    String headerJson = "{\"alg\":\"none\"}";
    String payloadJson = "{\"email\":\"" + email + "\"}";

    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

    return header + "." + payload + ".signature";
  }
}
