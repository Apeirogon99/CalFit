package com.coffiness.calfit.api.meetingRoom.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DELETE /api/v1/meeting-rooms/{meetingRoomId}")
public class DELETE_specs {

  @Test
  void 정상_삭제_시_204_No_Content를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    long meetingRoomId =
        createMeetingRoomAndGetId(
            meetingRoomFixture, context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);

    ApiResponse<Void> response =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), meetingRoomId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    long meetingRoomId =
        createMeetingRoomAndGetId(
            meetingRoomFixture, context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);

    ApiResponse<Void> response =
        meetingRoomFixture.delete(null, context.workspaceId(), meetingRoomId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인사_담당자가_아니면_삭제할_수_없다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    long meetingRoomId =
        createMeetingRoomAndGetId(
            meetingRoomFixture, context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);
    Long memberId =
        memberFixture.getMyMember(context.hrToken(), context.workspaceId()).getData().id();
    memberFixture.updateMember(
        memberId,
        new UpdateMemberRequest(MemberType.INTERVIEWER),
        context.hrToken(),
        context.workspaceId());

    ApiResponse<Void> response =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), meetingRoomId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_meetingRoomId이면_404_Not_Found를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<Void> response =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), 99999L);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 예약이_있는_회의실을_삭제하면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    long meetingRoomId =
        createMeetingRoomAndGetId(
            meetingRoomFixture, context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);

    ApiResponse<?> reservation =
        meetingRoomFixture.reserve(
            context.hrToken(),
            context.workspaceId(),
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));
    assertThat(reservation.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<Void> response =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), meetingRoomId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제한_회의실과_같은_이름으로_다시_생성할_수_있다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String name = "회의실 A";

    long meetingRoomId =
        createMeetingRoomAndGetId(
            meetingRoomFixture, context.hrToken(), context.workspaceId(), name, 1, 5);

    ApiResponse<Void> deleteResponse =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), meetingRoomId);
    ApiResponse<MeetingRoomResponse> recreateResponse =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), name, 2, 8);

    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreateResponse.getData()).isNotNull();
    assertThat(recreateResponse.getData().id()).isNotEqualTo(meetingRoomId);
    assertThat(recreateResponse.getData().name()).isEqualTo(name);
  }

  private long createMeetingRoomAndGetId(
      MeetingRoomFixture meetingRoomFixture,
      String token,
      String tenantId,
      String name,
      Integer location,
      Integer capacity) {
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, tenantId, name, location, capacity);
    return response.getData().id();
  }
}
