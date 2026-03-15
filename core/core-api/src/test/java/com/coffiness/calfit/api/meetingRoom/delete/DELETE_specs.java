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
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    long meetingRoomId = meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 5).getData().id();

    // Act
    ApiResponse<Void> response = meetingRoomFixture.delete(token, tenantId, meetingRoomId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long meetingRoomId =
        createMeetingRoomAndGetId(meetingRoomFixture, token, tenantId, "회의실 A", 1, 5);

    // Act
    ApiResponse<Void> response = meetingRoomFixture.delete(null, tenantId, meetingRoomId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인사_담당자가_아니면_삭제할_수_없다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long memberId = memberFixture.getMyMember(token, tenantId).getData().id();
    memberFixture.updateMember(
        memberId, new UpdateMemberRequest(MemberType.INTERVIEWER), token, tenantId);
    long meetingRoomId =
        createMeetingRoomAndGetId(meetingRoomFixture, token, tenantId, "회의실 A", 1, 5);

    // Act
    ApiResponse<Void> response = meetingRoomFixture.delete(token, tenantId, meetingRoomId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_meetingRoomId이면_404_Not_Found를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    // Act
    ApiResponse<Void> response = meetingRoomFixture.delete(token, tenantId, 99999L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 예약이_있는_회의실을_삭제하면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    long meetingRoomId =
        createMeetingRoomAndGetId(meetingRoomFixture, token, tenantId, "회의실 A", 1, 5);
    meetingRoomFixture.delete(token, tenantId, meetingRoomId);

    // Act
    ApiResponse<Void> response = meetingRoomFixture.delete(token, tenantId, meetingRoomId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 삭제한_회의실과_같은_이름으로_다시_생성할_수_있다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String name = "회의실 A";

    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, tenantId, name, 1, 5);

    ApiResponse<Void> deleteResponse = meetingRoomFixture.delete(token, tenantId, meetingRoomId);
    ApiResponse<MeetingRoomResponse> recreateResponse =
        meetingRoomFixture.create(token, tenantId, name, 2, 8);

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
