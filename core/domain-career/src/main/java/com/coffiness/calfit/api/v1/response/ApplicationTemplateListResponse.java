package com.coffiness.calfit.api.v1.response;

public record ApplicationTemplateListResponse(
    Long id,
    String name,
    String createdAt,
    String updatedAt,
    String status,
    boolean used,
    Long recruitmentCount,
    String customFields) {}
