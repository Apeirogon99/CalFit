package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * 대시보드 익명 채팅 메시지 생성 요청 DTO
 * */
public record DashboardChatMessageCreateRequest(
    @NotBlank(message = "메시지를 입력해주세요.") @Size(max = 500, message = "메시지는 500자 이하로 입력해주세요.")
        String content) {}
