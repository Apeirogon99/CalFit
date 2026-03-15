package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import java.time.LocalDateTime;

public record AnnouncementBoardResponse(
    Long id, String title, String content, Boolean pinned, LocalDateTime createdAt) {

  public static AnnouncementBoardResponse from(AnnouncementBoard announcementBoard) {
    return new AnnouncementBoardResponse(
        announcementBoard.id(),
        announcementBoard.title(),
        announcementBoard.content(),
        announcementBoard.pinned(),
        announcementBoard.createdAt());
  }
}
