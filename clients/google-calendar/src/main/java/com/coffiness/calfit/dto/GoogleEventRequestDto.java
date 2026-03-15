package com.coffiness.calfit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/*
 * 일정 생성 레코드
 * */
public record GoogleEventRequestDto(
    String summary, String description, EventDateTime start, EventDateTime end) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EventDateTime(
      String dateTime, // 일반 일정
      String date, // 종일 일정
      String timeZone) {}
}
