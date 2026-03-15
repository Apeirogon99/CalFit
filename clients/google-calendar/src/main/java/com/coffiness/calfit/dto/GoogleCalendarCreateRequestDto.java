package com.coffiness.calfit.dto;

/*
 * 전용 Google 캘린더를 생성할 때 사용하는 DTO
 * */
public record GoogleCalendarCreateRequestDto(String summary, String description, String timeZone) {}
