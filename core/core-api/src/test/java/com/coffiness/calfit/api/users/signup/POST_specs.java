package com.coffiness.calfit.api.users.signup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenRepository;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("POST /api/v1/users/signup")
public class POST_specs {

  @MockBean private EmailService emailService;

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired UserFixture fixture) {
    String email = fixture.randomEmail();
    String password = fixture.randomPassword();
    String name = fixture.randomName();

    ApiResponse<UserResponse> response = fixture.signUp(email, password, name);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 이메일_형식이_올바르지_않으면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    ApiResponse<UserResponse> response =
        fixture.signUp("invalid-email", fixture.randomPassword(), fixture.randomName());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 비밀번호가_4자_미만이면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    ApiResponse<UserResponse> response =
        fixture.signUp(fixture.randomEmail(), "123", fixture.randomName());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이름이_2자_미만이면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
    ApiResponse<UserResponse> response =
        fixture.signUp(fixture.randomEmail(), fixture.randomPassword(), "A");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 회원가입이_성공하면_사용자_정보를_올바르게_반환한다(@Autowired UserFixture fixture) {
    String email = fixture.randomEmail();
    String name = fixture.randomName();

    ApiResponse<UserResponse> response = fixture.signUp(email, fixture.randomPassword(), name);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().email()).isEqualTo(email);
    assertThat(response.getData().name()).isEqualTo(name);
  }

  @Test
  void 회원가입이_성공하면_사용자_ID를_반환한다(@Autowired UserFixture fixture) {
    ApiResponse<UserResponse> response = fixture.signUp();

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
  }

  @Test
  void 회원가입이_성공하면_인증_토큰을_생성하고_인증메일_발송을_요청한다(
      @Autowired UserFixture fixture, @Autowired EmailVerificationTokenRepository tokenRepository) {
    String email = fixture.randomEmail();

    ApiResponse<UserResponse> response =
        fixture.signUp(email, fixture.randomPassword(), fixture.randomName());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(tokenRepository.findByUserEmail(email)).isPresent();
    verify(emailService).sendWorkspaceVerificationEmail(anyString(), any());
  }
}
