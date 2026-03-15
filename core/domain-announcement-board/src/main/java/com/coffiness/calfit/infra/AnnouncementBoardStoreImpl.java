package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardStore;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardEntity;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementBoardStoreImpl implements AnnouncementBoardStore {

  private final AnnouncementBoardRepository announcementBoardRepository;

  @Override
  public AnnouncementBoard create(String title, String content, Boolean pinned) {
    AnnouncementBoardEntity entity =
        AnnouncementBoardEntity.builder().title(title).content(content).pinned(pinned).build();
    AnnouncementBoardEntity saved = announcementBoardRepository.save(entity);

    return new AnnouncementBoard(
        saved.getId(),
        saved.getTitle(),
        saved.getContent(),
        saved.getPinned(),
        saved.getCreatedAt());
  }

  @Override
  public AnnouncementBoard update(
      Long announcementBoardId, String title, String content, Boolean pinned) {
    AnnouncementBoardEntity entity =
        announcementBoardRepository
            .findByIdAndStatus(announcementBoardId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new NoSuchElementException("announcement board not found"));

    entity.update(title, content, pinned);

    return new AnnouncementBoard(
        entity.getId(),
        entity.getTitle(),
        entity.getContent(),
        entity.getPinned(),
        entity.getCreatedAt());
  }

  @Override
  public void delete(Long announcementBoardId) {
    AnnouncementBoardEntity entity =
        announcementBoardRepository
            .findByIdAndStatus(announcementBoardId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new NoSuchElementException("announcement board not found"));
    entity.deleted();
  }
}
