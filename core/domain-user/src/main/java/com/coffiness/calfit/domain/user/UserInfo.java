package com.coffiness.calfit.domain.user;

import java.time.LocalDateTime;

/** 외부 도메인에 제공하는 User 정보 DTO */
public record UserInfo(
    Long id,
    String email,
    String name,
    String role,
    LocalDateTime createdAt,
    LocalDateTime recentAt) {}
