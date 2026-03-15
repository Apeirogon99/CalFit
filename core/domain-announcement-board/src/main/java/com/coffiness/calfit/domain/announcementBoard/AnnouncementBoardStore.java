package com.coffiness.calfit.domain.announcementBoard;

public interface AnnouncementBoardStore {

  AnnouncementBoard create(String title, String content, Boolean pinned);

  AnnouncementBoard update(Long announcementBoardId, String title, String content, Boolean pinned);

  void delete(Long announcementBoardId);
}
