package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.PendingInterviewStage;
import java.util.List;

/*
 * 면접 단계별 대기 지원자 현황을 내려주는 응답 DTO
 * */
public record InterviewPendingStageResponse(
    Long recruitmentStageId,
    String stageName,
    Integer stageStep,
    Integer pendingApplicantCount,
    List<InterviewPendingApplicantResponse> applicants) {

  // 도메인 단계 모델을 API 응답 형식으로 변환
  public static InterviewPendingStageResponse from(PendingInterviewStage stage) {
    return new InterviewPendingStageResponse(
        stage.recruitmentStageId(),
        stage.stageName(),
        stage.stageStep(),
        stage.pendingApplicantCount(),
        stage.applicants().stream().map(InterviewPendingApplicantResponse::from).toList());
  }
}
