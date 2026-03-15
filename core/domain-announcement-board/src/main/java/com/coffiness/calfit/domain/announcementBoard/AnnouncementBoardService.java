package com.coffiness.calfit.domain.announcementBoard;

import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementBoardService {

  private final AnnouncementBoardReader announcementBoardReader;
  private final AnnouncementBoardStore announcementBoardStore;
  private final AnnouncementBoardValidator announcementBoardValidator;
  private final DomainEventPublisher domainEventPublisher;

  /* 공지사항 생성 */
  @Transactional
  public AnnouncementBoard create(String title, String content, Boolean pinned, Long userId) {
    announcementBoardValidator.validateCreateRequest(title, content, pinned, userId);
    AnnouncementBoard announcementBoard = announcementBoardStore.create(title, content, pinned);
    domainEventPublisher.publish(
        AnnouncementCreatedEvent.of(
            currentTenantId(), announcementBoard.id(), announcementBoard.title(), userId));
    return announcementBoard;
  }

  /* 공지사항 조회 */
  @Transactional(readOnly = true)
  public List<AnnouncementBoard> list() {
    return announcementBoardReader.getAnnouncementBoards();
  }

  @Transactional(readOnly = true)
  public AnnouncementBoard get(Long id) {
    return announcementBoardReader
        .findActiveBoard(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  /* 공지사항 수정 */
  @Transactional
  public AnnouncementBoard update(
      Long id, String title, String content, Boolean pinned, Long userId) {
    announcementBoardValidator.validateUpdateRequest(id, title, content, pinned, userId);
    return announcementBoardStore.update(id, title, content, pinned);
  }

  /* 공지사항 삭제 */
  @Transactional
  public void delete(Long id, Long userId) {
    announcementBoardValidator.validateDeleteRequest(id, userId);
    announcementBoardStore.delete(id);
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }
}
