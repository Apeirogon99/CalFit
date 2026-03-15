package com.coffiness.calfit.api.meetingRoom.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
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
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "회의실 A";
    Integer location = 1;
    Integer capacity = 10;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
  }

  @Test
  void 인증_토큰이_없으면_401을_반환한다(@Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = "";
    String name = "회의실 A";
    Integer location = 1;
    Integer capacity = 10;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_없으면_400을_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = null;
    Integer location = 1;
    Integer capacity = 10;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_2자_미만이면_400을_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "A";
    Integer location = 1;
    Integer capacity = 10;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_50자를_초과하면_400을_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "a".repeat(51);
    Integer location = 1;
    Integer capacity = 10;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 수용인원이_없으면_400을_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "회의실 A";
    Integer location = 1;
    Integer capacity = null;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 수용인원이_1미만이면_400을_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "회의실 A";
    Integer location = 1;
    Integer capacity = 0;

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 같은_워크스페이스_내_중복된_이름이면_409를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    String name = "회의실 A";
    Integer location = 1;
    Integer capacity = 10;

    ApiResponse<MeetingRoomResponse> first =
        meetingRoomFixture.create(token, name, location, capacity);

    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    ApiResponse<MeetingRoomResponse> second =
        meetingRoomFixture.create(token, name, location, capacity);

    // Assert
    assertThat(second.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 설명과_시설_색상도_함께_저장된다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    String token = userFixture.createUserAndGetToken();

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(
            token,
            null,
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
  void 삭제한_회의실과_같은_이름으로_새_회의실을_다시_생성할_수_있다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> first =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 10);

    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<Void> deleted = meetingRoomFixture.delete(token, tenantId, first.getData().id());

    assertThat(deleted.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomResponse> recreated =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 2, 12);

    assertThat(recreated.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(recreated.getData().id()).isNotEqualTo(first.getData().id());
    assertThat(recreated.getData().name()).isEqualTo("회의실 A");
  }
}
