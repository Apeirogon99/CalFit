package com.coffiness.calfit.api.calendar.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.*;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.*;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.response.ScheduleAvailabilityResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules/availability")
public class GET_specs {

  @Test
  void 선택한_참석자의_당일_바쁜_일정_목록을_조회한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired MemberFixture memberFixture,
      @Autowired CalendarFixture calendarFixture) {

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

    LocalDate targetDate = LocalDate.now().plusDays(1);
    LocalDateTime startTime = targetDate.atTime(10, 0);
    LocalDateTime endTime = targetDate.atTime(11, 0);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 기존 일정",
            "참석자가 이미 가지고 있는 바쁜 일정",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of());

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(attendeeToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleAvailabilityResponse> response =
        calendarFixture.getScheduleAvailability(
            ownerToken, tenantId, targetDate.toString(), List.of(attendeeUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().attendeeAvailabilities()).hasSize(1);

    ScheduleAvailabilityResponse.AttendeeAvailabilityResponse attendeeAvailability =
        response.getData().attendeeAvailabilities().get(0);

    assertThat(attendeeAvailability.attendeeId()).isEqualTo(attendeeUserId);
    assertThat(attendeeAvailability.attendeeName()).isEqualTo(attendeeName);
    assertThat(attendeeAvailability.busySchedules()).hasSize(1);
    assertThat(attendeeAvailability.busySchedules().get(0).title()).isEqualTo("참석자 기존 일정");
    assertThat(attendeeAvailability.busySchedules().get(0).isAllDay()).isFalse();
    assertThat(attendeeAvailability.busySchedules().get(0).type()).isEqualTo(ScheduleType.MEETING);
    assertThat(attendeeAvailability.busySchedules().get(0).interviewScheduleId()).isNull();
  }

  @Test
  void 본인_userId를_조회하면_본인_바쁜_일정도_포함한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String ownerToken = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(ownerToken).getData();
    String tenantId = workspace.workspaceId();
    Long ownerUserId = userFixture.me(ownerToken).getData().id();

    LocalDate targetDate = LocalDate.now().plusDays(1);
    LocalDateTime startTime = targetDate.atTime(14, 0);
    LocalDateTime endTime = targetDate.atTime(15, 0);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "내 기존 일정",
            "로그인한 사용자의 바쁜 일정",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of());

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(ownerToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleAvailabilityResponse> response =
        calendarFixture.getScheduleAvailability(
            ownerToken, tenantId, targetDate.toString(), List.of(ownerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().attendeeAvailabilities()).hasSize(1);

    ScheduleAvailabilityResponse.AttendeeAvailabilityResponse attendeeAvailability =
        response.getData().attendeeAvailabilities().get(0);

    assertThat(attendeeAvailability.attendeeId()).isEqualTo(ownerUserId);
    assertThat(attendeeAvailability.busySchedules()).hasSize(1);
    assertThat(attendeeAvailability.busySchedules().get(0).title()).isEqualTo("내 기존 일정");
    assertThat(attendeeAvailability.busySchedules().get(0).isAllDay()).isFalse();
    assertThat(attendeeAvailability.busySchedules().get(0).type()).isEqualTo(ScheduleType.MEETING);
    assertThat(attendeeAvailability.busySchedules().get(0).interviewScheduleId()).isNull();
  }

  @Test
  void 면접_자동생성_일정은_인터뷰_식별자를_포함해_조회한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired CalendarFixture calendarFixture) {

    String hrToken = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(hrToken).getData();
    String tenantId = workspace.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    String interviewerName = userFixture.randomName();
    userFixture.signUp(interviewerEmail, interviewerPassword, interviewerName);

    ApiResponse<LoginResponse> interviewerLoginResponse =
        userFixture.login(interviewerEmail, interviewerPassword);
    String interviewerToken = interviewerLoginResponse.getData().accessToken();
    Long interviewerUserId = interviewerLoginResponse.getData().user().id();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), interviewerToken);

    RecruitmentCreateRequest recruitmentRequest =
        new RecruitmentCreateRequest(
            "캘린더 연동 테스트 공고",
            1,
            1L,
            "테스트 공고",
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(interviewerUserId),
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));
    recruitmentFixture.createRecruitment(hrToken, tenantId, recruitmentRequest);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData().get(0);
    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    ApiResponse<InterviewResponse> createInterviewResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.id(),
            recruitment.stages().get(0).id(),
            InterviewRound.FIRST,
            List.of(interviewerUserId),
            List.of(301L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 14, 0),
            60,
            "실무 면접");
    assertThat(createInterviewResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleAvailabilityResponse> response =
        calendarFixture.getScheduleAvailability(
            hrToken, tenantId, LocalDate.of(2030, 3, 13).toString(), List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().attendeeAvailabilities()).hasSize(1);

    ScheduleAvailabilityResponse.BusyScheduleResponse busySchedule =
        response.getData().attendeeAvailabilities().get(0).busySchedules().get(0);

    assertThat(busySchedule.type()).isEqualTo(ScheduleType.INTERVIEW);
    assertThat(busySchedule.interviewScheduleId())
        .isEqualTo(createInterviewResponse.getData().id());
  }
}
