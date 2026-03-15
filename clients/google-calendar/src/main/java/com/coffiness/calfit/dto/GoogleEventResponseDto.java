package com.coffiness.calfit.dto;

import com.coffiness.calfit.model.GoogleCalendarClientResult;

/*
 * 일정 단일 생성 DTO
 * */
public record GoogleEventResponseDto(String id, String status) {
  public GoogleCalendarClientResult toResult() {
    return new GoogleCalendarClientResult(this.id(), "confirmed".equals(this.status()));
  }
}
