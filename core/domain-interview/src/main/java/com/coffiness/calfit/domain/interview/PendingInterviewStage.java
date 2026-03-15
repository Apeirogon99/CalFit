package com.coffiness.calfit.domain.interview;

import java.util.List;

/*
 * 면접 생성 화면에서 사용할 단계별 대기 지원자 정보를 담는 도메인 모델
 * */
public record PendingInterviewStage(
    Long recruitmentStageId,
    String stageName,
    Integer stageStep,
    List<PendingInterviewApplicant> applicants) {

  // 단계에 남아 있는 대기 지원자 수를 계산
  public int pendingApplicantCount() {
    return applicants == null ? 0 : applicants.size();
  }
}
