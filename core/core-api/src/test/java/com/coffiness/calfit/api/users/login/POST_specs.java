package com.coffiness.calfit.api.users.login;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/users/login")
public class POST_specs {

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    fixture.signUp(email, password, fixture.randomName());

    // Act
    ApiResponse<LoginResponse> response = fixture.login(email, password);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 존재하지_않는_이메일로_로그인하면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange & Act
    ApiResponse<LoginResponse> response =
        fixture.login("nonexistent@test.com", fixture.randomPassword());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 잘못된_비밀번호로_로그인하면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    fixture.signUp(email, fixture.randomPassword(), fixture.randomName());

    // Act
    ApiResponse<LoginResponse> response = fixture.login(email, "wrong-password");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 로그인_성공시_액세스_토큰을_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    fixture.signUp(email, password, fixture.randomName());

    // Act
    ApiResponse<LoginResponse> response = fixture.login(email, password);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().accessToken()).isNotBlank();
  }

  @Test
  void 로그인_성공시_리프레시_토큰을_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    fixture.signUp(email, password, fixture.randomName());

    // Act
    ApiResponse<LoginResponse> response = fixture.login(email, password);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().refreshToken()).isNotBlank();
  }

  @Test
  void 로그인_성공시_사용자_정보를_반환한다(@Autowired UserFixture fixture) {
    // Arrange
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    String name = fixture.randomName();
    fixture.signUp(email, password, name);

    // Act
    ApiResponse<LoginResponse> response = fixture.login(email, password);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().user()).isNotNull();
    assertThat(response.getData().user().email()).isEqualTo(email);
    assertThat(response.getData().user().name()).isEqualTo(name);
  }
}
