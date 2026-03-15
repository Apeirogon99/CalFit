package com.coffiness.calfit.domain.announcementBoard;

public interface AnnouncementBoardValidator {

  void validateCreateRequest(String title, String content, Boolean pinned, Long userId);

  void validateUpdateRequest(
      Long announcementBoardId, String title, String content, Boolean pinned, Long userId);

  void validateDeleteRequest(Long announcementBoardId, Long userId);
}
