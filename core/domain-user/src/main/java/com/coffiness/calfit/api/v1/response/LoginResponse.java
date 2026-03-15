package com.coffiness.calfit.api.v1.response;

public record LoginResponse(
    String accessToken, String refreshToken, UserResponse user, String workspaceId) {}
