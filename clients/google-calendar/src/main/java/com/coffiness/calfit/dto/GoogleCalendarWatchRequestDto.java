package com.coffiness.calfit.dto;

import java.util.Map;

/*
 * 구글 Calendar API watch 채널 생성 요청 DTO
 * */
public record GoogleCalendarWatchRequestDto(
    String id, String type, String address, String token, Map<String, String> params) {}
