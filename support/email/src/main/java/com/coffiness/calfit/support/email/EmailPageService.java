package com.coffiness.calfit.support.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailPageService {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final TemplateEngine templateEngine;

  public String renderSignupVerificationSuccessPage(String workspaceCreateUrl, String loginUrl) {
    Context context = new Context();
    context.setVariable("workspaceCreateUrl", workspaceCreateUrl);
    context.setVariable("loginUrl", loginUrl);
    return templateEngine.process("signup-verification-success-page", context);
  }

  public String renderSignupVerificationFailurePage(String loginUrl) {
    Context context = new Context();
    context.setVariable("loginUrl", loginUrl);
    return templateEngine.process("signup-verification-failure-page", context);
  }

  public String renderInvitationPage(
      String invitedEmail,
      String memberTypeLabel,
      LocalDateTime expiresAt,
      String acceptUrl,
      String loginUrl) {
    Context context = new Context();
    context.setVariable("invitedEmail", invitedEmail);
    context.setVariable("memberTypeLabel", memberTypeLabel);
    context.setVariable("expiresAtText", formatDateTime(expiresAt));
    context.setVariable("acceptUrl", acceptUrl);
    context.setVariable("loginUrl", loginUrl);
    return templateEngine.process("workspace-invitation-page", context);
  }

  public String renderInvitationFailurePage(String loginUrl) {
    Context context = new Context();
    context.setVariable("loginUrl", loginUrl);
    return templateEngine.process("workspace-invitation-failure-page", context);
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
  }
}
