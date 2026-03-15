package com.coffiness.calfit.api.meetingRoom.reserve;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/meeting-rooms/{meetingRoomId}/reservations")
class POST_specs {

  @Test
  void 정상_요청시_회의실_예약이_생성된다(
      @Autowired MemberFixture memberFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 1, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> response =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, start, end);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().meetingRoomId()).isEqualTo(meetingRoomId);
    assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount()).isOne();
  }

  @Test
  void 동일_시간대_중복_예약이면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 1, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> first =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, start, end);
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse> second =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 30),
            LocalDateTime.of(2030, 1, 10, 11, 30));

    assertThat(second.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 연속된_다음_시간대_예약은_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> first =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse> second =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 11, 0),
            LocalDateTime.of(2030, 1, 10, 12, 0));

    assertThat(second.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 시작시간과_종료시간이_같으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime sameTime = LocalDateTime.of(2030, 1, 10, 10, 0);
    ApiResponse<MeetingRoomReservationResponse> response =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, sameTime, sameTime);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접에_배정된_면접관은_겹치는_시간에_다른_회의실을_수동_예약할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired UserFixture userFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    userFixture.signUp(interviewerEmail, interviewerPassword, userFixture.randomName());

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token());

    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();
    Long interviewerUserId = userFixture.me(interviewerToken).getData().id();

    Long firstRoomId = meetingRoomFixture.create(hrToken, tenantId, "회의실 A", 1, 6).getData().id();
    Long secondRoomId = meetingRoomFixture.create(hrToken, tenantId, "회의실 B", 1, 6).getData().id();

    LocalDateTime scheduledAt = LocalDateTime.of(2030, 3, 11, 9, 0);
    RecruitmentCreateRequest recruitmentRequest =
        new RecruitmentCreateRequest(
            "상반기 백엔드 채용",
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

    recruitmentFixture.createRecruitment(hrToken, tenantId, recruitmentRequest);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData().get(0).id();

    ApiResponse<InterviewResponse> createdInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitmentId,
            null,
            InterviewRound.FIRST,
            List.of(interviewerUserId),
            List.of(101L),
            firstRoomId,
            scheduledAt,
            180,
            "상반기 백엔드 채용 실무 면접");

    assertThat(createdInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse> manualReservation =
        meetingRoomFixture.reserve(
            interviewerToken,
            tenantId,
            secondRoomId,
            LocalDateTime.of(2030, 3, 11, 10, 0),
            LocalDateTime.of(2030, 3, 11, 11, 0));

    assertThat(manualReservation.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 참석자도_예약목록에서_주최자명과_전체_참석자명을_확인할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired UserFixture userFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String organizerToken = context.hrToken();
    String tenantId = context.workspaceId();
    UserResponse organizer = userFixture.me(organizerToken).getData();

    String attendee1Email = userFixture.randomEmail();
    String attendee1Password = userFixture.randomPassword();
    String attendee1Name = "참석자1";
    Long attendee1UserId =
        userFixture.signUp(attendee1Email, attendee1Password, attendee1Name).getData().id();
    String attendee1Token =
        userFixture.login(attendee1Email, attendee1Password).getData().accessToken();
    ApiResponse<InvitationResponse> attendee1Invitation =
        memberFixture.createInvitation(
            organizerToken, tenantId, attendee1Email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(attendee1Invitation.getData().token(), attendee1Token);

    String attendee2Email = userFixture.randomEmail();
    String attendee2Password = userFixture.randomPassword();
    String attendee2Name = "참석자2";
    Long attendee2UserId =
        userFixture.signUp(attendee2Email, attendee2Password, attendee2Name).getData().id();
    String attendee2Token =
        userFixture.login(attendee2Email, attendee2Password).getData().accessToken();
    ApiResponse<InvitationResponse> attendee2Invitation =
        memberFixture.createInvitation(
            organizerToken, tenantId, attendee2Email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(attendee2Invitation.getData().token(), attendee2Token);

    Long meetingRoomId =
        meetingRoomFixture.create(organizerToken, tenantId, "회의실 A", 1, 6).getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 1, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> created =
        meetingRoomFixture.reserve(
            organizerToken,
            tenantId,
            meetingRoomId,
            "참석자 확인용 예약",
            "공유 예약 상세 테스트",
            start,
            end,
            List.of(attendee1UserId, attendee2UserId));

    assertThat(created.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse[]> reservations =
        meetingRoomFixture.listReservations(
            attendee1Token, tenantId, start.minusHours(1), end.plusHours(1));

    assertThat(reservations.getResult()).isEqualTo(ResultType.SUCCESS);

    Optional<MeetingRoomReservationResponse> reservation =
        List.of(reservations.getData()).stream()
            .filter(item -> created.getData().id().equals(item.id()))
            .findFirst();

    assertThat(reservation).isPresent();
    assertThat(reservation.get().organizerName()).isEqualTo(organizer.name());
    assertThat(reservation.get().attendees()).contains(attendee1Name, attendee2Name);
    assertThat(reservation.get().title()).isEqualTo("참석자 확인용 예약");
  }

  @Test
  void 수동_회의실_예약을_생성하면_주최자와_참석자_모두의_내_일정에서_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String organizerToken = context.hrToken();
    String tenantId = context.workspaceId();

    String attendeeEmail = userFixture.randomEmail();
    String attendeePassword = userFixture.randomPassword();
    String attendeeName = "회의 참석자";
    Long attendeeUserId =
        userFixture.signUp(attendeeEmail, attendeePassword, attendeeName).getData().id();
    String attendeeToken =
        userFixture.login(attendeeEmail, attendeePassword).getData().accessToken();
    ApiResponse<InvitationResponse> attendeeInvitation =
        memberFixture.createInvitation(
            organizerToken, tenantId, attendeeEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(attendeeInvitation.getData().token(), attendeeToken);

    Long meetingRoomId =
        meetingRoomFixture.create(organizerToken, tenantId, "일정 공유 회의실", 1, 6).getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 2, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2030, 2, 10, 16, 0);

    ApiResponse<MeetingRoomReservationResponse> created =
        meetingRoomFixture.reserve(
            organizerToken,
            tenantId,
            meetingRoomId,
            "수동 예약 일정",
            "참석자에게도 보여야 하는 일정",
            start,
            end,
            List.of(attendeeUserId));

    assertThat(created.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<ScheduleResponse>> organizerSchedules =
        calendarFixture.getSchedules(organizerToken, tenantId, "2030-02-10", "2030-02-10");
    ApiResponse<List<ScheduleResponse>> attendeeSchedules =
        calendarFixture.getSchedules(attendeeToken, tenantId, "2030-02-10", "2030-02-10");

    assertThat(organizerSchedules.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(attendeeSchedules.getResult()).isEqualTo(ResultType.SUCCESS);

    assertThat(organizerSchedules.getData())
        .extracting(ScheduleResponse::title)
        .contains("수동 예약 일정");
    assertThat(attendeeSchedules.getData())
        .extracting(ScheduleResponse::title)
        .contains("수동 예약 일정");
  }

  @Test
  void 연결된_일정이_없어도_회의실_예약_목록_조회는_실패하지_않는다(
      @Autowired MemberFixture memberFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired ScheduleRepository scheduleRepository) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    Long meetingRoomId =
        meetingRoomFixture.create(token, tenantId, "복구 테스트 회의실", 1, 6).getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 4, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 4, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> created =
        meetingRoomFixture.reserve(
            token, tenantId, meetingRoomId, "복구 테스트 예약", "설명", start, end, List.of());

    assertThat(created.getResult()).isEqualTo(ResultType.SUCCESS);

    List<ScheduleEntity> schedules =
        scheduleRepository.findAllByReservationIdAndStatus(
            created.getData().id(), EntityStatus.ACTIVE);
    assertThat(schedules).isNotEmpty();
    schedules.forEach(ScheduleEntity::deleted);
    scheduleRepository.saveAll(schedules);

    ApiResponse<MeetingRoomReservationResponse[]> reservations =
        meetingRoomFixture.listReservations(token, tenantId, start.minusHours(1), end.plusHours(1));

    assertThat(reservations.getResult()).isEqualTo(ResultType.SUCCESS);

    Optional<MeetingRoomReservationResponse> reservation =
        List.of(reservations.getData()).stream()
            .filter(item -> created.getData().id().equals(item.id()))
            .findFirst();

    assertThat(reservation).isPresent();
    assertThat(reservation.get().title()).isEqualTo("회의실 예약");
  }
}
