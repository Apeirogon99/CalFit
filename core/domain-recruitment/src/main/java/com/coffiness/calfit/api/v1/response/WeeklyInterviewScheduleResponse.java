package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.WeeklyInterviewScheduleItem;
import java.time.LocalDateTime;

/*
 * 주간 면접 일정 API 응답을 내려주는 DTO
 * */
public record WeeklyInterviewScheduleResponse(
    Long id,
    Long recruitmentId,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long interviewerUserId,
    String interviewerName,
    String title,
    String applicantName,
    String description,
    String location) {

  // 도메인 조회 모델을 API 응답 형식으로 변환
  public static WeeklyInterviewScheduleResponse from(WeeklyInterviewScheduleItem item) {
    return new WeeklyInterviewScheduleResponse(
        item.id(),
        item.recruitmentId(),
        item.startAt(),
        item.endAt(),
        item.interviewerUserId(),
        item.interviewerName(),
        item.title(),
        item.applicantName(),
        item.description(),
        item.location());
  }
}
