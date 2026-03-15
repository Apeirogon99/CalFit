package com.coffiness.calfit.api.meetingRoom.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
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
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    meetingRoomFixture.create(token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse[]> response = meetingRoomFixture.list(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다(@Autowired MeetingRoomFixture meetingRoomFixture) {
    // Act
    ApiResponse<MeetingRoomResponse[]> response = meetingRoomFixture.list();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 다른_사용자가_생성한_회의실도_포함된다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String tokenA = userFixture.createUserAndGetToken();
    String tokenB = userFixture.createUserAndGetToken();
    meetingRoomFixture.create(tokenA, "회의실 A", 1, 5);
    meetingRoomFixture.create(tokenB, "회의실 B", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse[]> response = meetingRoomFixture.list(tokenA);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isEqualTo(2);
  }

  @Test
  void 회의실_목록이_이름_오름차순으로_정렬된다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    meetingRoomFixture.create(token, "회의실 B", 1, 5);
    meetingRoomFixture.create(token, "회의실 A", 1, 5);

    // Act
    ApiResponse<MeetingRoomResponse[]> response = meetingRoomFixture.list(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isGreaterThanOrEqualTo(2);
    assertThat(response.getData()[0].name()).isEqualTo("회의실 A");
    assertThat(response.getData()[1].name()).isEqualTo("회의실 B");
  }

  @Test
  void 회의실이_없으면_빈_리스트를_반환한다(
      @Autowired UserFixture userFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<MeetingRoomResponse[]> response = meetingRoomFixture.list(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isEqualTo(0);
  }
}
