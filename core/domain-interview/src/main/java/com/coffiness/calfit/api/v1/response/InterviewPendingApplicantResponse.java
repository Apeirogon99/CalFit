package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.PendingInterviewApplicant;

/*
 * 면접 생성 화면에서 사용할 대기 지원자 응답 DTO
 * */
public record InterviewPendingApplicantResponse(
    Long applicationId, Long applicantId, String name, String email) {

  // 도메인 대기 지원자 모델을 API 응답으로 변환
  public static InterviewPendingApplicantResponse from(PendingInterviewApplicant applicant) {
    return new InterviewPendingApplicantResponse(
        applicant.applicationId(), applicant.applicantId(), applicant.name(), applicant.email());
  }
}
