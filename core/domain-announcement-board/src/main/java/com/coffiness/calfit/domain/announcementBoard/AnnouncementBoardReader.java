package com.coffiness.calfit.domain.announcementBoard;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementBoardReader {

  List<AnnouncementBoard> getAnnouncementBoards();

  Optional<AnnouncementBoard> findActiveBoard(Long announcementBoardId);
}
