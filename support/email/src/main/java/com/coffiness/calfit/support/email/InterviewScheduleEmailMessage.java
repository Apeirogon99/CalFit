package com.coffiness.calfit.support.email;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewScheduleEmailMessage {
  private final String applicantEmail;
  private final String applicantName;
  private final String subject;
  private final LocalDateTime scheduledAt;
  private final LocalDateTime endAt;
  private final String location;
}
