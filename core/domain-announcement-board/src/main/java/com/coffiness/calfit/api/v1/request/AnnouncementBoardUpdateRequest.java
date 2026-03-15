package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementBoardUpdateRequest(
    @NotBlank(message = "공지 제목은 필수입니다.")
        @Size(min = 2, max = 100, message = "공지 제목은 2자 이상 100자 이하여야 합니다.")
        String title,
    @NotBlank(message = "공지 내용은 필수입니다.") @Size(max = 2000, message = "공지 내용은 2000자 이하여야 합니다.")
        String content,
    @NotNull(message = "공지 고정 여부는 필수입니다.") Boolean pinned) {}
