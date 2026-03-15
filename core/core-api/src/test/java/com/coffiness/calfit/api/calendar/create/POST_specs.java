package com.coffiness.calfit.api.calendar.create;

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
import com.coffiness.calfit.api.v1.response.UserResponse;
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
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
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
@DisplayName("POST /api/v1/schedules")
public class POST_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 필수_데이터를_입력하면_내_일정_생성에_성공한다(
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
            "기획 회의",
            "프로젝트 초기 기획 회의입니다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 회의실을_포함하여_내_일정_생성에_성공한다(
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

    // Act
    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    var detailInfo = calendarFixture.getDetailSchedule(token, tenantId, scheduleId).getData();

    assertThat(detailInfo).isNotNull();
    assertThat(detailInfo.title()).isEqualTo("중요한 임원 회의");
    assertThat(detailInfo.roomId()).isEqualTo(roomId);
  }

  @Test
  void 워크스페이스에_속하지_않은_참석자가_포함되면_일정_생성에_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 검증 실패 테스트",
            "워크스페이스 외부 참석자 검증",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            List.of(999999L));

    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 참석자에게_같은_시간대의_바쁜_일정이_있으면_일정_생성에_실패한다(
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
    LocalDateTime startTime = now.plusDays(1).withHour(13).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest attendeeSchedule =
        new ScheduleCreateRequest(
            "참석자 바쁜 일정",
            "기존 바쁜 일정",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            null);
    ApiResponse<Void> attendeeCreateResponse =
        calendarFixture.createSchedule(attendeeToken, tenantId, attendeeSchedule);
    assertThat(attendeeCreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ScheduleCreateRequest ownerSchedule =
        new ScheduleCreateRequest(
            "겹치는 회의",
            "참석자 충돌 테스트",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of(attendeeUserId));

    ApiResponse<Void> response =
        calendarFixture.createSchedule(ownerToken, tenantId, ownerSchedule);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 참석자의_기존_일정이_한가함으로_표시되면_겹쳐도_일정_생성이_가능하다(
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
    LocalDateTime startTime = now.plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);

    ScheduleCreateRequest attendeeSchedule =
        new ScheduleCreateRequest(
            "참석자 한가한 일정",
            "기존 한가한 일정",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            false,
            null);
    ApiResponse<Void> attendeeCreateResponse =
        calendarFixture.createSchedule(attendeeToken, tenantId, attendeeSchedule);
    assertThat(attendeeCreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ScheduleCreateRequest ownerSchedule =
        new ScheduleCreateRequest(
            "겹침 허용 회의",
            "참석자 한가함 테스트",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of(attendeeUserId));

    ApiResponse<Void> response =
        calendarFixture.createSchedule(ownerToken, tenantId, ownerSchedule);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 중복된_참석자_ID가_전달되면_한_번만_저장한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    UserResponse me = userFixture.me(token).getData();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "중복 참석자 제거 테스트",
            "같은 참석자를 여러 번 보내도 한 번만 저장되어야 한다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            List.of(me.id(), me.id(), me.id()));

    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    var detailInfo = calendarFixture.getDetailSchedule(token, tenantId, scheduleId).getData();

    assertThat(detailInfo).isNotNull();
    assertThat(detailInfo.attendeeIds()).containsExactly(me.id());
  }

  @Test
  void 참석자를_초대해_일정을_생성하면_참석자의_내_일정에_바로_보이고_초대_알림이_발송된다(
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
    LocalDateTime startTime = now.plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endTime = startTime.plusHours(1);
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 초대 일정",
            "참석자 초대 일정 설명",
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

    assertThat(calendarFixture.getSchedules(attendeeToken, tenantId, startDate, endDate).getData())
        .anySatisfy(schedule -> assertThat(schedule.title()).isEqualTo("참석자 초대 일정"));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              ApiResponse<NotificationFixture.NotificationPageResponse> unreadResponse =
                  notificationFixture.unread(attendeeToken, tenantId, null, 0, 10);

              assertThat(unreadResponse.getData().contents())
                  .extracting(NotificationResponse::type)
                  .contains(NotificationType.SCHEDULE_INVITED);
            });
  }

  @Test
  void 일정_생성_후_구글_이벤트_ID로_재동기화하면_한_건만_유지된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String authCode = "google-auth-code-create";
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
        .willReturn(new GoogleCalendarClientResult("google-created-event-1", true));

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "캘핏에서 생성한 일정",
            "아웃바운드 동기화 테스트",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> createResponse =
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

    ScheduleSyncRequest syncRequest =
        new ScheduleSyncRequest(
            "google-created-event-1",
            "구글에서 재동기화된 제목",
            "구글에서 재동기화된 설명",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(1),
            false);

    ApiResponse<Void> syncResponse = calendarFixture.syncSchedule(token, tenantId, syncRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    var schedules = calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();

    // Assert
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(syncResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules).hasSize(1);
    assertThat(schedules.get(0).title()).isEqualTo("구글에서 재동기화된 제목");
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
