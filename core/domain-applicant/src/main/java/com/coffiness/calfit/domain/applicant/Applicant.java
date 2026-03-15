package com.coffiness.calfit.domain.applicant;

import java.time.LocalDateTime;

public record Applicant(
    Long id, String workspaceId, String email, String name, LocalDateTime createdAt) {}
