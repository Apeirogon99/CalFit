package com.coffiness.calfit.api.v1.response;

public record PresignUploadResponse(
    Long fileId, String uploadUrl, String objectKey, long expiresInMinutes) {}
