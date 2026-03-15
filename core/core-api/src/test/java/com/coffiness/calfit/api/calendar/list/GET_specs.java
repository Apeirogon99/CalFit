package com.coffiness.calfit.api.calendar.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules")
public class GET_specs {

  @Test
  @DisplayName("특정 기간의 일정을 조회하면 리스트를 반환한다")
  void returns_schedule_list_in_selected_period(
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

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(token, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    String startDate = now.toLocalDate().minusDays(1).toString();
    String endDate = now.toLocalDate().plusDays(7).toString();
    ApiResponse<List<ScheduleResponse>> response =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().size()).isGreaterThan(0);
    assertThat(response.getData().get(0).title()).isEqualTo("기획 회의");
  }

  @Test
  @DisplayName("참석자로 지정된 사용자는 자신의 일정 목록에서 해당 일정을 조회할 수 있다")
  void attendee_can_view_shared_schedule_in_schedule_list(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired MemberFixture memberFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String ownerToken = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(ownerToken).getData();
    String tenantId = workspace.workspaceId();

    String attendeeEmail = userFixture.randomEmail();
    String attendeePassword = userFixture.randomPassword();
    String attendeeName = userFixture.randomName();

    userFixture.signUp(attendeeEmail, attendeePassword, attendeeName);

    ApiResponse<LoginResponse> attendeeLoginResponse =
        userFixture.login(attendeeEmail, attendeePassword);
    String attendeeToken = attendeeLoginResponse.getData().accessToken();
    Long attendeeUserId = attendeeLoginResponse.getData().user().id();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, attendeeEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), attendeeToken);

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 공유 일정",
            "참석자 일정 반영 테스트",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            List.of(attendeeUserId));

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(ownerToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    String startDate = now.toLocalDate().minusDays(1).toString();
    String endDate = now.toLocalDate().plusDays(7).toString();
    ApiResponse<List<ScheduleResponse>> response =
        calendarFixture.getSchedules(attendeeToken, tenantId, startDate, endDate);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData()).extracting(ScheduleResponse::title).contains("참석자 공유 일정");
  }

  @Test
  @DisplayName("회의실이 지정된 일정은 목록 조회 시 장소 정보를 함께 반환한다")
  void returns_location_in_schedule_list(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long roomId = meetingRoomFixture.create(token, tenantId, "소회의실 A", 3, 6).getData().id();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "회의실 일정",
            "장소 포함 응답 확인",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            roomId,
            false,
            null);

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(token, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().minusDays(1).toString();
    String endDate = now.toLocalDate().plusDays(7).toString();
    ApiResponse<List<ScheduleResponse>> response =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData()).extracting(ScheduleResponse::location).contains("소회의실 A");
  }
}
