package com.coffiness.calfit.api.interview.availability;

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
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/interviews/availability")
class GET_specs {

  @Test
  void 토큰이_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            null, tenantId, LocalDateTime.now().plusDays(1), List.of(), List.of());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void from_파라미터가_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availabilityWithoutFrom(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접관이_생성한_일반_일정도_가용시간_조회에_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    Long interviewerUserId =
        userFixture.signUp(interviewerEmail, interviewerPassword, "면접관 사용자").getData().id();
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, interviewerToken);

    LocalDateTime start =
        LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(1);

    ApiResponse<Void> createScheduleResponse =
        calendarFixture.createSchedule(
            interviewerToken,
            tenantId,
            new ScheduleCreateRequest(
                "면접관 개인 일정",
                "면접 준비 시간",
                ScheduleType.MEETING,
                start,
                end,
                false,
                null,
                true,
                List.of()));

    assertThat(createScheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken, tenantId, start.minusHours(1), List.of(), List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .anySatisfy(
            slot -> {
              assertThat(slot.interviewerId()).isEqualTo(interviewerUserId);
              assertThat(slot.start()).isEqualTo(start);
              assertThat(slot.end()).isEqualTo(end);
            });
  }

  @Test
  void 면접관이_공유_일정의_참석자인_경우도_가용시간_조회에_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    Long interviewerUserId =
        userFixture.signUp(interviewerEmail, interviewerPassword, "공유 일정 면접관").getData().id();
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, interviewerToken);

    LocalDateTime start =
        LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(2);

    ApiResponse<Void> createScheduleResponse =
        calendarFixture.createSchedule(
            hrToken,
            tenantId,
            new ScheduleCreateRequest(
                "면접관 공유 일정",
                "면접관을 참석자로 포함한 일정",
                ScheduleType.MEETING,
                start,
                end,
                false,
                null,
                true,
                List.of(interviewerUserId)));

    assertThat(createScheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken, tenantId, start.minusHours(1), List.of(), List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .anySatisfy(
            slot -> {
              assertThat(slot.interviewerId()).isEqualTo(interviewerUserId);
              assertThat(slot.start()).isEqualTo(start);
              assertThat(slot.end()).isEqualTo(end);
            });
  }

  @Test
  void 면접관이_수동_회의실_예약의_참석자인_경우도_가용시간_조회에_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    Long interviewerUserId =
        userFixture.signUp(interviewerEmail, interviewerPassword, "회의실 예약 면접관").getData().id();
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, interviewerToken);

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "회의실 A", 3, 6).getData().id();
    LocalDateTime start =
        LocalDateTime.now().plusDays(4).withHour(15).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(1);

    assertThat(
            meetingRoomFixture.reserve(
                hrToken,
                tenantId,
                meetingRoomId,
                "회식",
                "면접관이 참석자인 수동 회의실 예약",
                start,
                end,
                List.of(interviewerUserId)))
        .extracting(ApiResponse::getResult)
        .isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken,
            tenantId,
            start.minusHours(1),
            List.of(meetingRoomId),
            List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .anySatisfy(
            slot -> {
              assertThat(slot.interviewerId()).isEqualTo(interviewerUserId);
              assertThat(slot.start()).isEqualTo(start);
              assertThat(slot.end()).isEqualTo(end);
            });
  }

  @Test
  void 면접_일정이_내_일정에_반영되어도_면접관_가용시간에는_중복_집계되지_않는다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "중복 확인 면접관");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "플랫폼 채용",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    LocalDateTime scheduledAt = LocalDateTime.of(2030, 3, 13, 10, 0);
    ApiResponse<InterviewResponse> createResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoomId,
            scheduledAt,
            60,
            "플랫폼 면접");

    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken, tenantId, scheduledAt.minusHours(1), List.of(), List.of(interviewer.userId()));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .filteredOn(
            slot ->
                slot.interviewerId().equals(interviewer.userId())
                    && slot.start().equals(scheduledAt)
                    && slot.end().equals(scheduledAt.plusHours(1)))
        .hasSize(1);
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
    String token = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);

    return new InterviewerContext(userId, token);
  }

  // 테스트용 채용 공고와 면접 단계를 생성
  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      String title,
      List<Long> interviewerIds,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            1,
            1L,
            "채용 공고",
            startDate,
            endDate,
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            interviewerIds,
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    return new RecruitmentContext(recruitment.id(), recruitment.stages().get(0).id());
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(Long recruitmentId, Long stageId) {}
}
