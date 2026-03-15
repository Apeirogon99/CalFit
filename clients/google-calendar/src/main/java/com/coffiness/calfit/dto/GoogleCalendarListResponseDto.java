package com.coffiness.calfit.dto;

import java.util.List;

/*
 * Google 캘린더 목록 조회 API 응답을 담는 DTO
 * */
public record GoogleCalendarListResponseDto(List<Item> items, String nextPageToken) {

  // Google 캘린더 목록의 개별 항목 1개를 나타내는 DTO
  public record Item(
      String id, String summary, String description, String accessRole, Boolean deleted) {}
}
