package com.coffiness.calfit.api.meetingRoom.reserveCancel;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewAvailabilityResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DELETE /api/v1/meeting-rooms/{meetingRoomId}/reservations/{reservationId}")
class DELETE_specs {

  @Test
  void 예약_취소에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> reservation =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(
            token, tenantId, meetingRoomId, reservation.getData().id());

    assertThat(canceled.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(canceled.getData().status()).isEqualTo(MeetingRoomStatus.DELETED);
  }

  @Test
  void 존재하지_않는_예약_취소시_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(token, tenantId, meetingRoomId, 99999L);

    assertThat(canceled.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 토큰이_없으면_예약_취소에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> reservation =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(
            null, tenantId, meetingRoomId, reservation.getData().id());

    assertThat(canceled.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접_예약을_취소하면_연결된_면접일정과_가용시간도_함께_해제된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, token, tenantId, "취소 확인 면접관");
    Long interviewerUserId = interviewer.userId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime scheduledAt = LocalDateTime.of(2030, 3, 10, 12, 0);
    RecruitmentCreateRequest recruitmentRequest =
        new RecruitmentCreateRequest(
            "백엔드 개발자 채용",
            1,
            1L,
            "채용 공고",
            scheduledAt.minusDays(10),
            scheduledAt.plusDays(10),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(interviewerUserId),
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));

    recruitmentFixture.createRecruitment(token, tenantId, recruitmentRequest);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    ApiResponse<InterviewResponse> createdInterview =
        interviewFixture.create(
            token,
            tenantId,
            recruitmentId,
            null,
            InterviewRound.FIRST,
            List.of(interviewerUserId),
            List.of(101L),
            meetingRoomId,
            scheduledAt,
            60,
            "면접관: 테스트 면접관\n지원자: 홍길동");

    assertThat(createdInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<com.coffiness.calfit.v1.response.ScheduleResponse>> beforeCancelSchedules =
        calendarFixture.getSchedules(interviewer.token(), tenantId, "2030-03-10", "2030-03-10");

    assertThat(beforeCancelSchedules.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(beforeCancelSchedules.getData()).hasSize(1);

    ApiResponse<MeetingRoomReservationResponse[]> reservations =
        meetingRoomFixture.listReservations(
            token, tenantId, scheduledAt.minusHours(1), scheduledAt.plusHours(2));

    Optional<MeetingRoomReservationResponse> interviewReservation =
        Arrays.stream(reservations.getData())
            .filter(
                reservation ->
                    createdInterview.getData().id().equals(reservation.interviewScheduleId()))
            .findFirst();

    assertThat(interviewReservation).isPresent();

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(
            token, tenantId, meetingRoomId, interviewReservation.get().id());

    assertThat(canceled.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(canceled.getData().status()).isEqualTo(MeetingRoomStatus.DELETED);
    assertThat(canceled.getData().interviewScheduleId()).isEqualTo(createdInterview.getData().id());

    ApiResponse<List<InterviewScheduleResponse>> schedules =
        recruitmentFixture.getRecruitmentSchedule(token, tenantId, recruitmentId, "2030-03");

    assertThat(schedules.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules.getData()).isEmpty();

    ApiResponse<InterviewAvailabilityResponse> availability =
        interviewFixture.availability(
            token,
            tenantId,
            scheduledAt.minusDays(1),
            List.of(meetingRoomId),
            List.of(interviewerUserId));

    assertThat(availability.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(availability.getData().meetingRoomBusySlots()).isEmpty();
    assertThat(availability.getData().interviewerBusySlots()).isEmpty();

    ApiResponse<List<com.coffiness.calfit.v1.response.ScheduleResponse>> afterCancelSchedules =
        calendarFixture.getSchedules(interviewer.token(), tenantId, "2030-03-10", "2030-03-10");

    assertThat(afterCancelSchedules.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(afterCancelSchedules.getData()).isEmpty();
  }

  // 테스트용 면접관 사용자를 생성하고 워크스페이스에 초대
  private InterviewerContext inviteInterviewer(
      MemberFixture memberFixture,
      UserFixture userFixture,
      String hrToken,
      String tenantId,
      String name) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    Long userId = userFixture.signUp(email, password, name).getData().id();
    String userToken = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), userToken);

    return new InterviewerContext(userId, userToken);
  }

  private record InterviewerContext(Long userId, String token) {}
}
