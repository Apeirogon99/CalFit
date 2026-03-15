package com.coffiness.calfit.v1.request;

import jakarta.validation.constraints.NotBlank;

public record CalendarConnectRequest(
    @NotBlank(message = "구글 인가 코드는 필수입니다.") String authCode,
    @NotBlank(message = "리다이렉트 URI는 필수입니다.") String redirectUri) {}
