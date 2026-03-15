package com.coffiness.calfit.api.v1.response;

public record CompleteUploadResponse(Long fileId, String uploadStatus, Long sizeBytes) {}
