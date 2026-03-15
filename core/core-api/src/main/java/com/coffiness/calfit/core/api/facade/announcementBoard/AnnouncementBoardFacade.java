package com.coffiness.calfit.core.api.facade.announcementBoard;

import com.coffiness.calfit.api.v1.request.AnnouncementBoardCreateRequest;
import com.coffiness.calfit.api.v1.request.AnnouncementBoardUpdateRequest;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AnnouncementBoardFacade {

  private final AnnouncementBoardService announcementBoardService;

  @Transactional
  public AnnouncementBoard createAnnouncement(Long userId, AnnouncementBoardCreateRequest request) {
    return announcementBoardService.create(
        request.title(), request.content(), request.pinned(), userId);
  }

  @Transactional(readOnly = true)
  public List<AnnouncementBoard> getAnnouncementList() {
    return announcementBoardService.list();
  }

  @Transactional(readOnly = true)
  public AnnouncementBoard getAnnouncement(Long announcementBoardId) {
    return announcementBoardService.get(announcementBoardId);
  }

  @Transactional
  public AnnouncementBoard updateAnnouncement(
      Long userId, Long announcementBoardId, AnnouncementBoardUpdateRequest request) {
    return announcementBoardService.update(
        announcementBoardId, request.title(), request.content(), request.pinned(), userId);
  }

  @Transactional
  public void deleteAnnouncement(Long userId, Long announcementBoardId) {
    announcementBoardService.delete(announcementBoardId, userId);
  }
}
