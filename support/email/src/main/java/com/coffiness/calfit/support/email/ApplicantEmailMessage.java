package com.coffiness.calfit.support.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantEmailMessage {
  private final String applicantEmail;
  private final String applicantName;
  private final String companyName;
  private final String positionName;
  private final boolean accepted;
  private final String subject;
  private final String headline;
  private final String summary;
  private final String highlightLabel;
  private final String highlightValue;
}
