package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import java.time.LocalDateTime;

public record AnnouncementBoardListResponse(
    Long id, String title, String content, Boolean pinned, LocalDateTime createdAt) {

  public static AnnouncementBoardListResponse from(AnnouncementBoard announcementBoard) {
    return new AnnouncementBoardListResponse(
        announcementBoard.id(),
        announcementBoard.title(),
        announcementBoard.content(),
        announcementBoard.pinned(),
        announcementBoard.createdAt());
  }
}
