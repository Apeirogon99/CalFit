package com.coffiness.calfit.api.v1.response;

import java.time.LocalDateTime;

/*
 * 대시보드 익명 채팅 메시지 응답 DTO
 * */
public record DashboardChatMessageResponse(
    Long id, String alias, String content, LocalDateTime createdAt, boolean mine) {}
