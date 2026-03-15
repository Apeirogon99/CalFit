package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardReader;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardEntity;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementBoardReaderImpl implements AnnouncementBoardReader {

  private final AnnouncementBoardRepository announcementBoardRepository;

  @Override
  public List<AnnouncementBoard> getAnnouncementBoards() {
    return announcementBoardRepository
        .findAllByStatusOrderByPinnedDescCreatedAtDesc(EntityStatus.ACTIVE)
        .stream()
        .map(this::toAnnouncementBoard)
        .toList();
  }

  @Override
  public Optional<AnnouncementBoard> findActiveBoard(Long announcementBoardId) {
    return announcementBoardRepository
        .findByIdAndStatus(announcementBoardId, EntityStatus.ACTIVE)
        .map(this::toAnnouncementBoard);
  }

  private AnnouncementBoard toAnnouncementBoard(AnnouncementBoardEntity entity) {
    return new AnnouncementBoard(
        entity.getId(),
        entity.getTitle(),
        entity.getContent(),
        entity.getPinned(),
        entity.getCreatedAt());
  }
}
