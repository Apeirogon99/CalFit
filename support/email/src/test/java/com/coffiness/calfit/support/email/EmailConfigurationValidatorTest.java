package com.coffiness.calfit.support.email;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

class EmailConfigurationValidatorTest {

  @Test
  void shouldThrowWhenSmtpPasswordIsMissing() {
    EmailProperties emailProperties = new EmailProperties();
    emailProperties.setEnabled(true);
    emailProperties.setSender("no-reply@calfit.com");

    MailProperties mailProperties = new MailProperties();
    mailProperties.setHost("smtp.naver.com");
    mailProperties.setUsername("no-reply@calfit.com");
    mailProperties.getProperties().put("mail.smtp.auth", "true");

    EmailConfigurationValidator validator =
        new EmailConfigurationValidator(emailProperties, mailProperties);

    assertThatThrownBy(validator::validateOrThrow)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SPRING_MAIL_PASSWORD");
  }

  @Test
  void shouldAllowMissingCredentialsWhenSmtpAuthIsDisabled() {
    EmailProperties emailProperties = new EmailProperties();
    emailProperties.setEnabled(true);
    emailProperties.setSender("no-reply@calfit.com");

    MailProperties mailProperties = new MailProperties();
    mailProperties.setHost("localhost");
    mailProperties.getProperties().put("mail.smtp.auth", "false");

    EmailConfigurationValidator validator =
        new EmailConfigurationValidator(emailProperties, mailProperties);

    assertThatCode(validator::validateOrThrow).doesNotThrowAnyException();
  }

  @Test
  void shouldSkipValidationWhenEmailIsDisabled() {
    EmailProperties emailProperties = new EmailProperties();
    emailProperties.setEnabled(false);

    EmailConfigurationValidator validator =
        new EmailConfigurationValidator(emailProperties, new MailProperties());

    assertThatCode(validator::validateOrThrow).doesNotThrowAnyException();
  }
}
