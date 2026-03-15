package com.coffiness.calfit.domain.user;

import java.time.LocalDateTime;

/** User 도메인 모델 */
public record User(Long id, String email, String name, String role, LocalDateTime createdAt) {}
