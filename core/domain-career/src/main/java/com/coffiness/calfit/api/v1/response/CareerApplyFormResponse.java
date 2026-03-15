package com.coffiness.calfit.api.v1.response;

public record CareerApplyFormResponse(
    Long recruitmentId, String title, Long templateId, Long firstStageId, String customFields) {}
