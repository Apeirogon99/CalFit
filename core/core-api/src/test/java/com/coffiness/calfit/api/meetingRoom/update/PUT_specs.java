package com.coffiness.calfit.api.meetingRoom.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
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
@DisplayName("PUT /api/v1/meeting-rooms/{meetingRoomId}")
public class PUT_specs {

  @Test
  void 올바른_요청_시_200_OK를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "회의실 B", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(null, meetingRoomId, "회의실 B", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_사용자라도_수정이_가능하다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String tokenA = userFixture.createUserAndGetToken();
    String tokenB = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, tokenA, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(tokenB, meetingRoomId, "회의실 B", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 존재하지_않는_meetingRoomId이면_404_Not_Found를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, 99999L, "회의실 B", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_없으면_400_Bad_Request를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, null, 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_2자_미만이면_400_Bad_Request를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "A", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void name_속성이_50자_초과이면_400_Bad_Request를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "a".repeat(51), 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void capacity_속성이_없으면_400_Bad_Request를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "회의실 B", 18, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void capacity_속성이_1_미만이면_400_Bad_Request를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "회의실 B", 18, 0);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 같은_워크스페이스_내_동일한_회의실_이름이면_409_Conflict를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 B", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(token, meetingRoomId, "회의실 A", 18, 10);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 설명과_시설_색상도_수정할_수_있다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    String token = userFixture.createUserAndGetToken();
    long meetingRoomId = createMeetingRoomAndGetId(meetingRoomFixture, token, "회의실 A", 1, 5);

    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.update(
            token,
            null,
            meetingRoomId,
            "회의실 B",
            18,
            10,
            "설명 수정",
            List.of("WiFi", "프로젝터"),
            "#ef4444");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().location()).isEqualTo(18);
    assertThat(response.getData().description()).isEqualTo("설명 수정");
    assertThat(response.getData().facilities()).containsExactly("WiFi", "프로젝터");
    assertThat(response.getData().color()).isEqualTo("#ef4444");
  }

  private long createMeetingRoomAndGetId(
      MeetingRoomFixture meetingRoomFixture,
      String token,
      String name,
      Integer location,
      Integer capacity) {
    ApiResponse<MeetingRoomResponse> response =
        meetingRoomFixture.create(token, name, location, capacity);
    return response.getData().id();
  }
}
