package com.coffiness.calfit.domain.recruitment;

// 상태 전이 결과(오픈/마감 처리 건수) 전달용 값 객체
public record RecruitmentStatusTransitionResult(int openedCount, int closedCount) {}
