package com.coffiness.calfit.support.email;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConfigurationValidator implements ApplicationRunner {

  private static final String MISSING_CONFIGURATION_MESSAGE =
      "이메일 발송이 활성화되어 있지만 필요한 메일 설정이 없습니다: %s. " + "환경 변수 또는 import된 .env 파일에 값을 설정한 뒤 다시 시도해 주세요.";

  private final EmailProperties emailProperties;
  private final MailProperties springMailProperties;

  @Override
  public void run(ApplicationArguments args) {
    if (emailProperties.isEnabled()) {
      List<String> missingSettings = findMissingSettings();
      if (missingSettings.isEmpty()) {
        log.info("이메일 설정 검증이 완료되었습니다. host={}", springMailProperties.getHost());
      } else {
        log.warn(
            "이메일 발송이 활성화되어 있지만 필요한 메일 설정이 없습니다: {}. "
                + "환경 변수 또는 import된 .env 파일에 값을 설정한 뒤 다시 시도해 주세요. "
                + "user.dir 기준으로 확인한 .env 후보 경로는 다음과 같습니다. user.dir={}, candidates={}",
            String.join(", ", missingSettings),
            System.getProperty("user.dir"),
            expectedEnvCandidates());
      }
    } else {
      log.info("이메일 발송이 비활성화되어 있습니다.");
    }
  }

  public void validateOrThrow() {
    if (!emailProperties.isEnabled()) {
      return;
    }

    List<String> missingSettings = findMissingSettings();
    if (!missingSettings.isEmpty()) {
      throw new IllegalStateException(
          MISSING_CONFIGURATION_MESSAGE.formatted(String.join(", ", missingSettings)));
    }
  }

  private List<String> findMissingSettings() {
    List<String> missingSettings = new ArrayList<>();

    if (!StringUtils.hasText(springMailProperties.getHost())) {
      missingSettings.add("SPRING_MAIL_HOST");
    }

    if (isSmtpAuthEnabled()) {
      if (!StringUtils.hasText(springMailProperties.getUsername())) {
        missingSettings.add("SPRING_MAIL_USERNAME");
      }
      if (!StringUtils.hasText(springMailProperties.getPassword())) {
        missingSettings.add("SPRING_MAIL_PASSWORD");
      }
    }

    if (!StringUtils.hasText(emailProperties.getSender())
        && !StringUtils.hasText(springMailProperties.getUsername())) {
      missingSettings.add("APP_EMAIL_SENDER or SPRING_MAIL_USERNAME");
    }

    return missingSettings;
  }

  private boolean isSmtpAuthEnabled() {
    String smtpAuth = springMailProperties.getProperties().get("mail.smtp.auth");
    if (!StringUtils.hasText(smtpAuth)) {
      return true;
    }
    return Boolean.parseBoolean(smtpAuth);
  }

  private String expectedEnvCandidates() {
    String userDir = System.getProperty("user.dir");
    return List.of(userDir + "/.env", userDir + "/../.env", userDir + "/../../.env").stream()
        .map(path -> path.replace("\\", "/"))
        .collect(Collectors.joining(", "));
  }
}
