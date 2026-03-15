package com.coffiness.calfit.api.v1.request;

public record PresignUploadRequest(
    Long applicationId,
    Long applicantId,
    String fieldKey,
    String originalFilename,
    String contentType) {}
