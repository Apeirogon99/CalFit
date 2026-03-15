package com.coffiness.calfit.domain.interview;

/*
 * 면접 단계에서 아직 일정이 잡히지 않은 지원자 정보를 담는 도메인 모델
 * */
public record PendingInterviewApplicant(
    Long applicationId, Long applicantId, String name, String email) {}
