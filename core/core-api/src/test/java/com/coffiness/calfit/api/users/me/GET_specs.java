package com.coffiness.calfit.api.users.me;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/users/me")
public class GET_specs {

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String token = fixture.createUserAndGetToken();

    // Act
    ApiResponse<UserResponse> response = fixture.me(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 인증_토큰_없이_요청하면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange & Act
    ApiResponse<UserResponse> response = fixture.base().get("/api/v1/users/me", UserResponse.class);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 잘못된_토큰으로_요청하면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange & Act
    ApiResponse<UserResponse> response = fixture.me("invalid-token");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 내_정보를_올바르게_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    String name = fixture.randomName();
    String token = fixture.createUserAndGetToken(email, password, name);

    // Act
    ApiResponse<UserResponse> response = fixture.me(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
    assertThat(response.getData().email()).isEqualTo(email);
    assertThat(response.getData().name()).isEqualTo(name);
  }
}
