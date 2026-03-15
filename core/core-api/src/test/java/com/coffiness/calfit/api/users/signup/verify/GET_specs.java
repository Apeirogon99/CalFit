package com.coffiness.calfit.api.users.signup.verify;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenEntity;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenRepository;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("GET /api/v1/users/signup/verify")
public class GET_specs {

  @MockBean private EmailService emailService;

  @Test
  void valid_token_marks_email_as_verified_and_renders_success_page(
      @Autowired UserFixture fixture,
      @Autowired EmailVerificationTokenRepository tokenRepository,
      @Autowired TestRestTemplate restTemplate) {
    String email = fixture.randomEmail();
    fixture.signUp(email, fixture.randomPassword(), fixture.randomName());

    EmailVerificationTokenEntity token = tokenRepository.findByUserEmail(email).orElseThrow();

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/api/v1/users/signup/verify?token=" + token.getToken(), String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("회원가입이 완료되었습니다!");
    assertThat(tokenRepository.findByToken(token.getToken()))
        .get()
        .extracting(EmailVerificationTokenEntity::isVerified)
        .isEqualTo(true);
  }
}
