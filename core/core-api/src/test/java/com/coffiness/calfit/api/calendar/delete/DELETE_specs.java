package com.coffiness.calfit.api.calendar.delete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("DELETE /api/v1/schedules/{scheduleId}")
public class DELETE_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 특정_일정을_삭제할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "삭제용 일정",
            "삭제용 일정",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();

    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    // Act
    ApiResponse<Void> deleteSchedule = calendarFixture.deleteSchedule(token, tenantId, scheduleId);

    // Assert
    assertThat(deleteSchedule.getResult()).isEqualTo(ResultType.SUCCESS);

    int sizeAfterDelete =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().size();
    assertThat(sizeAfterDelete).isEqualTo(0);
  }

  @Test
  void 회의실이_연동된_일정을_삭제하면_정상적으로_삭제된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long roomId = meetingRoomFixture.create(token, tenantId, "회의실1", 1, 10).getData().id();

    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "중요한 임원 회의",
            "회의실 연동 테스트입니다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            roomId,
            false,
            null);

    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    // Act
    ApiResponse<Void> deleteResponse = calendarFixture.deleteSchedule(token, tenantId, scheduleId);

    // Assert
    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    var existingSchedule =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();
    assertThat(existingSchedule).isEmpty();
  }

  @Test
  void 참석자를_초대한_일정을_삭제하면_참석자의_내_일정에서_바로_사라지고_취소_알림이_발송된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MemberFixture memberFixture,
      @Autowired NotificationFixture notificationFixture) {

    String ownerToken = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(ownerToken).getData();
    String tenantId = workspace.workspaceId();

    String attendeeEmail = userFixture.randomEmail();
    String attendeePassword = userFixture.randomPassword();
    userFixture.signUp(attendeeEmail, attendeePassword, userFixture.randomName());

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, attendeeEmail, MemberType.INTERVIEWER);
    String attendeeToken =
        userFixture.login(attendeeEmail, attendeePassword).getData().accessToken();
    memberFixture.acceptInvitation(invitationResponse.getData().token(), attendeeToken);
    Long attendeeUserId = userFixture.me(attendeeToken).getData().id();

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = now.plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "삭제 전 일정",
            "삭제 전 일정 설명",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of(attendeeUserId));

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(ownerToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture
            .getSchedules(ownerToken, tenantId, startDate, endDate)
            .getData()
            .get(0)
            .id();

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture
                            .unread(attendeeToken, tenantId, null, 0, 10)
                            .getData()
                            .contents())
                    .extracting(NotificationResponse::type)
                    .contains(NotificationType.SCHEDULE_INVITED));
    notificationFixture.readAll(attendeeToken, tenantId);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture
                            .unreadCount(attendeeToken, tenantId)
                            .getData()
                            .unreadCount())
                    .isZero());

    ApiResponse<Void> deleteResponse =
        calendarFixture.deleteSchedule(ownerToken, tenantId, scheduleId);

    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(calendarFixture.getSchedules(attendeeToken, tenantId, startDate, endDate).getData())
        .noneSatisfy(schedule -> assertThat(schedule.id()).isEqualTo(scheduleId));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture
                            .unread(attendeeToken, tenantId, null, 0, 10)
                            .getData()
                            .contents())
                    .extracting(NotificationResponse::type)
                    .contains(NotificationType.SCHEDULE_CANCELLED));
  }

  @Test
  void 구글_이벤트_ID가_있는_일정을_삭제하면_구글_일정도_삭제된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String authCode = "google-auth-code-delete";
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

    given(
            googleCalendarPort.createEvent(
                anyString(), anyString(), anyString(), anyString(), any(), any(), anyBoolean()))
        .willReturn(new GoogleCalendarClientResult("google-delete-event-1", true));

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "삭제 전 일정",
            "삭제 전 설명",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    calendarFixture.createSchedule(token, tenantId, createRequest);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                verify(googleCalendarPort, times(1))
                    .createEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyBoolean()));

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    // Act
    ApiResponse<Void> deleteResponse = calendarFixture.deleteSchedule(token, tenantId, scheduleId);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                verify(googleCalendarPort, times(1))
                    .deleteEvent(anyString(), anyString(), eq("google-delete-event-1")));

    // Assert
    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);
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
