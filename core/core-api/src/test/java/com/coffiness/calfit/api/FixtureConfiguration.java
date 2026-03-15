package com.coffiness.calfit.api;

import com.coffiness.calfit.api.fixture.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@TestConfiguration
public class FixtureConfiguration {

  @Bean
  @Scope("prototype")
  UserFixture userFixture(Environment environment, ObjectMapper objectMapper) {
    return UserFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  WorkspaceFixture workspaceFixture(Environment environment, ObjectMapper objectMapper) {
    return WorkspaceFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  MemberFixture memberFixture(Environment environment, ObjectMapper objectMapper) {
    return MemberFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  CalendarFixture calendarFixture(Environment environment, ObjectMapper objectMapper) {
    return CalendarFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  MeetingRoomFixture meetingRoomFixture(Environment environment, ObjectMapper objectMapper) {
    return MeetingRoomFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  AnnouncementBoardFixture announcementBoardFixture(
      Environment environment, ObjectMapper objectMapper) {
    return AnnouncementBoardFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  NotificationFixture notificationFixture(Environment environment, ObjectMapper objectMapper) {
    return NotificationFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  InterviewFixture interviewFixture(Environment environment, ObjectMapper objectMapper) {
    return InterviewFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  BillingFixture billingFixture(Environment environment, ObjectMapper objectMapper) {
    return BillingFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  RecruitmentFixture recruitmentFixture(Environment environment, ObjectMapper objectMapper) {
    return RecruitmentFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  PaymentFixture paymentFixture(Environment environment, ObjectMapper objectMapper) {
    return PaymentFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  ApplicantFixture applicantFixture(Environment environment, ObjectMapper objectMapper) {
    return ApplicantFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  ApplicationFileFixture applicationFileFixture(
      Environment environment, ObjectMapper objectMapper) {
    return ApplicationFileFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  ApplicationFixture applicationFixture(Environment environment, ObjectMapper objectMapper) {
    return ApplicationFixture.create(environment, objectMapper);
  }
}
