package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateGroupRequest(
    @NotBlank String name,
    @Pattern(
            regexp = "^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})$",
            message = "색상은 HEX 형식이어야 합니다 (예: #FF5733)")
        String color) {}
