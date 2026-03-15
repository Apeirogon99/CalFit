package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;

/*
 * 주간 면접 일정 카드와 모달에서 함께 쓰는 조회 모델
 * */
public record WeeklyInterviewScheduleItem(
    Long id,
    Long recruitmentId,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long interviewerUserId,
    String interviewerName,
    String title,
    String applicantName,
    String description,
    String location) {}
