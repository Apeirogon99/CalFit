package com.coffiness.calfit.api.calendar.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CalendarSyncReason;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.sync.policy.CalendarSyncDecision;
import com.coffiness.calfit.domain.sync.policy.CalendarSyncPolicy;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/calendars/google/connect")
public class POST_specs {

  private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 3, 17, 10, 0);

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  private final CalendarSyncPolicy calendarSyncPolicy = new CalendarSyncPolicy(APP_ZONE_ID);

  @Test
  void 올바른_인가코드로_요청하면_캘린더_연동에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    String validAuthCode = "dummy-auth-code";

    given(
            googleOAuthPort.exchangeAuthorizationCode(
                validAuthCode, "http://localhost:5173/auth/callback"))
        .willReturn(
            new OAuthExchangeResult(
                "mock-access", 3600L, "mock-refresh", createIdToken("test@gmail.com")));

    given(googleCalendarPort.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .willReturn("workspace-calendar-id");

    // Act
    ApiResponse<String> response =
        calendarFixture.connectGoogleCalendar(token, tenantId, validAuthCode);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 구글_연동에_성공하면_기존_캘핏_일정도_구글로_백필된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    String authCode = "backfill-auth-code";

    LocalDateTime now = LocalDateTime.now().plusDays(1);
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "연동 전 일정",
            "연동 즉시 구글로 반영되어야 하는 일정",
            ScheduleType.MEETING,
            now,
            now.plusHours(1),
            false,
            null,
            true,
            null);

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(token, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    given(
            googleOAuthPort.exchangeAuthorizationCode(
                authCode, "http://localhost:5173/auth/callback"))
        .willReturn(
            new OAuthExchangeResult(
                "mock-access", 3600L, "mock-refresh", createIdToken("backfill@test.com")));

    given(googleCalendarPort.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .willReturn("workspace-calendar-id");

    given(
            googleCalendarPort.createEvent(
                anyString(), anyString(), anyString(), anyString(), any(), any(), anyBoolean()))
        .willReturn(new GoogleCalendarClientResult("google-backfill-event-1", true));

    // Act
    ApiResponse<String> response = calendarFixture.connectGoogleCalendar(token, tenantId, authCode);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    verify(googleCalendarPort, times(1))
        .createEvent(
            anyString(),
            anyString(),
            eq("연동 전 일정"),
            eq("연동 즉시 구글로 반영되어야 하는 일정"),
            any(),
            any(),
            eq(false));
  }

  @Test
  void 인가코드가_비어있으면_에러_응답을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    String emptyAuthCode = "";

    given(googleCalendarPort.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .willReturn("workspace-calendar-id");

    // Act
    ApiResponse<String> response =
        calendarFixture.connectGoogleCalendar(token, tenantId, emptyAuthCode);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 구글_updatedAt이_없으면_일정을_반영한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = FIXED_TIME;
    LocalDateTime lastCalfitPushedAt = FIXED_TIME;

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(calfitUpdatedAt, lastCalfitPushedAt, null);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_MISSING_GOOGLE_TIMESTAMP);
  }

  @Test
  void calfit_푸시_직후_구글_변경은_에코로_무시한다() {
    // Arrange
    LocalDateTime now = FIXED_TIME;
    LocalDateTime calfitUpdatedAt = now;
    LocalDateTime lastCalfitPushedAt = now;
    ZonedDateTime googleUpdatedAt = now.plusSeconds(30).atZone(APP_ZONE_ID);

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isFalse();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.IGNORE_ECHO_FROM_CALFIT);
  }

  @Test
  void calfit_updatedAt이_없으면_구글_일정을_반영한다() {
    // Arrange
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt = FIXED_TIME.plusMinutes(5).atZone(APP_ZONE_ID);

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(null, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_CALFIT_TIMESTAMP_MISSING);
  }

  @Test
  void 구글이_명확히_최신이_아니면_일정을_무시한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = FIXED_TIME;
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt = calfitUpdatedAt.plusSeconds(3).atZone(APP_ZONE_ID);

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isFalse();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.IGNORE_GOOGLE_NOT_NEWER_THAN_CALFIT);
  }

  @Test
  void 구글이_충분히_최신이면_일정을_반영한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = FIXED_TIME;
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt = calfitUpdatedAt.plusSeconds(8).atZone(APP_ZONE_ID);

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_GOOGLE_NEWER_THAN_CALFIT);
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
