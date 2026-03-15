package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;

public record ApplicationFileResponse(
    Long fileId, String fieldKey, String originalFilename, String contentType, Long sizeBytes) {

  public static ApplicationFileResponse from(ApplicationFileEntity entity) {
    return new ApplicationFileResponse(
        entity.getId(),
        entity.getFieldKey(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes());
  }
}
