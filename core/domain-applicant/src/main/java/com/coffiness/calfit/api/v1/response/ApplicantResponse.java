package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.applicant.Applicant;
import java.time.LocalDateTime;

public record ApplicantResponse(
    Long id, String workspaceId, String email, String name, LocalDateTime createdAt) {

  public static ApplicantResponse from(Applicant applicant) {
    return new ApplicantResponse(
        applicant.id(),
        applicant.workspaceId(),
        applicant.email(),
        applicant.name(),
        applicant.createdAt());
  }
}
