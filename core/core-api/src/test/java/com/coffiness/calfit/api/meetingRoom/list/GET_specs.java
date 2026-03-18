package com.coffiness.calfit.api.meetingRoom.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/meeting-rooms")
public class GET_specs {

  @Test
  void 정상_요청_시_200_OK와_회의실_목록을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);

    ApiResponse<MeetingRoomResponse[]> response =
        meetingRoomFixture.list(context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse[]> response =
        meetingRoomFixture.list(null, context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_HR_멤버가_생성한_회의실도_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    userFixture.signUp(email, password, userFixture.randomName());
    String invitedToken = userFixture.login(email, password).getData().accessToken();
    ApiResponse<InvitationResponse> invitation =
        memberFixture.createInvitation(
            context.hrToken(), context.workspaceId(), email, MemberType.HR);
    memberFixture.acceptInvitation(invitation.getData().token(), invitedToken);

    meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);
    meetingRoomFixture.create(invitedToken, context.workspaceId(), "회의실 B", 1, 5);

    ApiResponse<MeetingRoomResponse[]> response =
        meetingRoomFixture.list(context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isEqualTo(2);
  }

  @Test
  void 회의실_목록이_이름_오름차순으로_정렬된다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 B", 1, 5);
    meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 5);

    ApiResponse<MeetingRoomResponse[]> response =
        meetingRoomFixture.list(context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isGreaterThanOrEqualTo(2);
    assertThat(response.getData()[0].name()).isEqualTo("회의실 A");
    assertThat(response.getData()[1].name()).isEqualTo("회의실 B");
  }

  @Test
  void 회의실이_없으면_빈_리스트를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse[]> response =
        meetingRoomFixture.list(context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isEqualTo(0);
  }
}
