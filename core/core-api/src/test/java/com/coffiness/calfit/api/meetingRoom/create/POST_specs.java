package com.coffiness.calfit.api.meetingRoom.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/meeting-rooms")
class POST_specs {

  @Test
  void 정상_요청시_회의실이_생성된다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
  }

  @Test
  void 인증_토큰이_없으면_401을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(null, context.workspaceId(), "회의실 A", 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), null, 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_2자_미만이면_400을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "A", 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_20자를_초과하면_400을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "a".repeat(21), 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 수용인원이_없으면_400을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, null);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 수용인원이_2미만이면_400을_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 1);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 같은_워크스페이스_내_중복된_이름이면_409를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> first =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 10);
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomResponse> second =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 10);

    assertThat(second.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 설명과_시설_색상도_함께_저장된다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(
            context.hrToken(),
            context.workspaceId(),
            "회의실 메타",
            18,
            12,
            "대형 모니터가 있는 회의실",
            List.of("WiFi", "모니터", "화이트보드"),
            "#3b82f6");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().location()).isEqualTo(18);
    assertThat(response.getData().description()).isEqualTo("대형 모니터가 있는 회의실");
    assertThat(response.getData().facilities()).containsExactly("WiFi", "모니터", "화이트보드");
    assertThat(response.getData().color()).isEqualTo("#3b82f6");
  }

  @Test
  void 인터뷰어는_회의실을_생성할_수_없다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    Long memberId =
        memberFixture.getMyMember(context.hrToken(), context.workspaceId()).getData().id();
    memberFixture.updateMember(
        memberId,
        new UpdateMemberRequest(MemberType.INTERVIEWER),
        context.hrToken(),
        context.workspaceId());

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "권한 없음 회의실", 1, 10);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제한_회의실과_같은_이름으로_새_회의실을_다시_생성할_수_있다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<MeetingRoomResponse> first =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 1, 10);
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<Void> deleted =
        meetingRoomFixture.delete(context.hrToken(), context.workspaceId(), first.getData().id());
    assertThat(deleted.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomResponse> recreated =
        meetingRoomFixture.create(context.hrToken(), context.workspaceId(), "회의실 A", 2, 12);

    assertThat(recreated.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreated.getData().id()).isNotEqualTo(first.getData().id());
    assertThat(recreated.getData().name()).isEqualTo("회의실 A");
  }
}
