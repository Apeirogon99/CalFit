package com.coffiness.calfit.api.v1.response;

public record PresignDownloadResponse(String downloadUrl, long expiresInMinutes) {}
