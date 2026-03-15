package com.coffiness.calfit.support.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final String SIGNUP_VERIFICATION_SUBJECT =
      "[CalFit] \uC774\uBA54\uC77C \uC778\uC99D\uC744 \uC644\uB8CC\uD574 \uC8FC\uC138\uC694";

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final TemplateEngine templateEngine;
  private final EmailProperties emailProperties;
  private final MailProperties springMailProperties;

  private void sendHtmlEmail(String from, String to, String subject, String htmlContent) {
    if (!emailProperties.isEnabled()) {
      log.info("Email sending is disabled. to={}", to);
      return;
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      throw new IllegalStateException(
          "JavaMailSender is not configured. Check SPRING_MAIL_HOST and related settings.");
    }

    String effectiveFrom = resolveFromAddress(from);
    if (!StringUtils.hasText(effectiveFrom)) {
      throw new IllegalStateException(
          "Sender address is empty. Configure APP_EMAIL_SENDER or SPRING_MAIL_USERNAME.");
    }

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(effectiveFrom);
      if (StringUtils.hasText(from) && !effectiveFrom.equalsIgnoreCase(from.trim())) {
        helper.setReplyTo(from.trim());
      }
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(mimeMessage);
      log.info("Email delivered. from={} to={} subject={}", effectiveFrom, to, subject);
    } catch (MessagingException | MailException e) {
      log.error("Failed to deliver email. from={} to={} subject={}", effectiveFrom, to, subject, e);
    }
  }

  private String resolveFromAddress(String requestedFrom) {
    String normalizedRequestedFrom = normalize(requestedFrom);
    String smtpUsername = normalize(springMailProperties.getUsername());

    if (!StringUtils.hasText(normalizedRequestedFrom)) {
      return smtpUsername;
    }
    if (!StringUtils.hasText(smtpUsername)) {
      return normalizedRequestedFrom;
    }

    if (isStrictSmtpProvider(springMailProperties.getHost())
        && !normalizedRequestedFrom.equalsIgnoreCase(smtpUsername)) {
      log.warn(
          "Override sender to SMTP username for provider compatibility. requestedFrom={}, smtpUsername={}",
          normalizedRequestedFrom,
          smtpUsername);
      return smtpUsername;
    }

    return normalizedRequestedFrom;
  }

  private boolean isStrictSmtpProvider(String host) {
    if (!StringUtils.hasText(host)) {
      return false;
    }
    String normalizedHost = host.trim().toLowerCase();
    return normalizedHost.contains("gmail") || normalizedHost.contains("naver");
  }

  private String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  @Async
  public void sendApplicantResultEmail(String from, ApplicantEmailMessage message) {
    Context context = new Context();
    context.setVariable("applicantName", message.getApplicantName());
    context.setVariable("companyName", message.getCompanyName());
    context.setVariable("positionName", message.getPositionName());
    context.setVariable("isAccepted", message.isAccepted());
    context.setVariable("headline", message.getHeadline());
    context.setVariable("summary", message.getSummary());
    context.setVariable("highlightLabel", message.getHighlightLabel());
    context.setVariable("highlightValue", message.getHighlightValue());

    String htmlContent = templateEngine.process("applicant-result-email", context);
    sendHtmlEmail(from, message.getApplicantEmail(), message.getSubject(), htmlContent);
  }

  @Async
  public void sendInterviewScheduleEmail(String from, InterviewScheduleEmailMessage message) {
    Context context = new Context();
    context.setVariable("applicantName", message.getApplicantName());
    context.setVariable("dateText", formatDate(message.getScheduledAt()));
    context.setVariable("timeText", formatTimeRange(message.getScheduledAt(), message.getEndAt()));
    context.setVariable("location", message.getLocation());

    String htmlContent = templateEngine.process("interview-schedule-email", context);
    sendHtmlEmail(from, message.getApplicantEmail(), message.getSubject(), htmlContent);
  }

  @Async
  public void sendWorkspaceVerificationEmail(
      String from, WorkspaceVerificationEmailMessage message) {
    Context context = new Context();
    context.setVariable("userName", message.getUserName());
    context.setVariable(
        "verificationLink",
        message.getVerificationUrl() + "?token=" + message.getVerificationToken());
    context.setVariable("expiresAtText", formatDateTime(message.getExpiresAt()));

    String htmlContent = templateEngine.process("workspace-verification-email", context);
    sendHtmlEmail(from, message.getUserEmail(), SIGNUP_VERIFICATION_SUBJECT, htmlContent);
  }

  @Async
  public void sendInvitationEmail(String from, InvitationEmailMessage message) {
    Context context = new Context();
    context.setVariable("workspaceName", message.getWorkspaceName());
    context.setVariable("inviterName", message.getInviterName());
    context.setVariable("memberTypeLabel", message.getMemberTypeLabel());
    context.setVariable("invitationLink", message.getInvitationUrl());
    context.setVariable("expiresAtText", formatDateTime(message.getExpiresAt()));

    String htmlContent = templateEngine.process("workspace-invitation-email", context);
    sendHtmlEmail(from, message.getInvitedEmail(), message.getSubject(), htmlContent);
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
  }

  private String formatDate(LocalDateTime value) {
    return value == null ? "-" : value.format(DATE_FORMATTER);
  }

  private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
    if (start == null) {
      return "-";
    }
    String startText = start.format(TIME_FORMATTER);
    if (end == null) {
      return startText;
    }
    return startText + " - " + end.format(TIME_FORMATTER);
  }
}
