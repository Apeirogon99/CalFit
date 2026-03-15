package com.coffiness.calfit.api.calendar.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
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
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
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
@DisplayName("PUT /api/v1/schedules/{scheduleId}")
public class PUT_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 특정_일정의_정보를_수정할_수_있다(
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
            "수정용 회의",
            "수정용 회의 description",
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

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정된 제목",
            "수정된 Description",
            ScheduleType.BUSINESS,
            now.plusDays(3),
            now.plusDays(3).plusHours(5),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(token, tenantId, scheduleId);

    assertThat(detailResponse.getData().title()).isEqualTo("수정된 제목");
    assertThat(detailResponse.getData().description()).isEqualTo("수정된 Description");
    assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.BUSINESS);
  }

  @Test
  void 기존_회의실_예약을_취소하고_새로운_회의실을_예약할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long oldRoomId = meetingRoomFixture.create(token, tenantId, "기존 회의실", 1, 10).getData().id();
    Long newRoomId = meetingRoomFixture.create(token, tenantId, "새 회의실", 1, 10).getData().id();

    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "기존 회의",
            "기존 회의 description",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            oldRoomId,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "변경될 회의",
            "회의실도 변경됨",
            ScheduleType.MEETING,
            now.plusDays(1).plusHours(2),
            now.plusDays(1).plusHours(4),
            false,
            newRoomId,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(token, tenantId, scheduleId);

    assertThat(detailResponse.getData().title()).isEqualTo("변경될 회의");
    assertThat(detailResponse.getData().roomId()).isEqualTo(newRoomId);
  }

  @Test
  void 워크스페이스에_속하지_않은_참석자가_포함되면_일정_수정에_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정 검증용 일정",
            "참석자 검증 테스트",
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

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정 검증용 일정",
            "참석자 검증 테스트",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            List.of(999999L));

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 동일한_일정을_같은_시간으로_수정할_때_자기_자신은_충돌로_보지_않는다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = now.plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "자기 자신 제외 테스트",
            "기존 시간 유지",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            null);
    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(token, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "제목만 변경", "시간 유지", ScheduleType.MEETING, startTime, endTime, false, null, true, null);

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 참석자에게_같은_시간대의_바쁜_일정이_있으면_일정_수정에_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MemberFixture memberFixture) {

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
    LocalDateTime conflictStart =
        now.plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime conflictEnd = conflictStart.plusHours(1);

    ScheduleCreateRequest attendeeSchedule =
        new ScheduleCreateRequest(
            "참석자 바쁜 일정",
            "수정 충돌 테스트",
            ScheduleType.MEETING,
            conflictStart,
            conflictEnd,
            false,
            null,
            true,
            null);
    ApiResponse<Void> attendeeCreateResponse =
        calendarFixture.createSchedule(attendeeToken, tenantId, attendeeSchedule);
    assertThat(attendeeCreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ScheduleCreateRequest ownerSchedule =
        new ScheduleCreateRequest(
            "기존 일정",
            "초기 시간은 겹치지 않음",
            ScheduleType.MEETING,
            now.plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0),
            now.plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
            false,
            null,
            true,
            List.of(attendeeUserId));
    ApiResponse<Void> ownerCreateResponse =
        calendarFixture.createSchedule(ownerToken, tenantId, ownerSchedule);
    assertThat(ownerCreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId =
        calendarFixture
            .getSchedules(ownerToken, tenantId, startDate, endDate)
            .getData()
            .get(0)
            .id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정된 일정",
            "이제 참석자와 겹침",
            ScheduleType.MEETING,
            conflictStart,
            conflictEnd,
            false,
            null,
            true,
            List.of(attendeeUserId));

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(ownerToken, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 참석자가_포함된_일정을_수정하면_참석자_화면에도_같은_일정이_갱신되고_수정_알림이_발송된다(
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
    LocalDateTime startTime = now.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정 전 일정",
            "수정 전 일정 설명",
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

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정 후 일정",
            "수정 후 일정 설명",
            ScheduleType.BUSINESS,
            startTime.plusHours(2),
            endTime.plusHours(2),
            false,
            null,
            true,
            List.of(attendeeUserId));

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(ownerToken, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(
            calendarFixture
                .getDetailSchedule(attendeeToken, tenantId, scheduleId)
                .getData()
                .title())
        .isEqualTo("수정 후 일정");

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
                    .contains(NotificationType.SCHEDULE_UPDATED));
  }

  @Test
  void 일정_수정_시_참석자에서_제외하면_참석자의_내_일정에서_사라지고_취소_알림이_발송된다(
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
    LocalDateTime startTime = now.plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 제외 일정",
            "참석자 제외 일정 설명",
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

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "참석자 제외 일정",
            "참석자 제외 일정 설명",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of());

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(ownerToken, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
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
  void 구글_이벤트_ID가_있는_일정을_수정하면_구글_일정도_수정된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String authCode = "google-auth-code-update";
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
        .willReturn(new GoogleCalendarClientResult("google-update-event-1", true));

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정 전 일정",
            "수정 전 설명",
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

    given(
            googleCalendarPort.updateEvent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                anyBoolean()))
        .willReturn(new GoogleCalendarClientResult("google-update-event-1", true));

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정 후 일정",
            "수정 후 설명",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                verify(googleCalendarPort, times(1))
                    .updateEvent(
                        anyString(),
                        anyString(),
                        eq("google-update-event-1"),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyBoolean()));

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
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
