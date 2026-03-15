package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardReader;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardValidator;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementBoardValidatorImpl implements AnnouncementBoardValidator {

  private final AnnouncementBoardReader announcementBoardReader;
  private final MemberReader memberReader;

  @Override
  public void validateCreateRequest(String title, String content, Boolean pinned, Long userId) {
    validateCommonFields(title, content, pinned);
    validateHrMember(userId);
  }

  @Override
  public void validateUpdateRequest(
      Long announcementBoardId, String title, String content, Boolean pinned, Long userId) {
    validateCommonFields(title, content, pinned);
    validateHrMember(userId);
    requireActiveBoard(announcementBoardId);
  }

  @Override
  public void validateDeleteRequest(Long announcementBoardId, Long userId) {
    validateHrMember(userId);
    requireActiveBoard(announcementBoardId);
  }

  private void validateCommonFields(String title, String content, Boolean pinned) {
    if (title == null
        || title.isBlank()
        || title.length() < 2
        || title.length() > 100
        || content == null
        || content.isBlank()
        || content.length() > 2000
        || pinned == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private AnnouncementBoard requireActiveBoard(Long announcementBoardId) {
    if (announcementBoardId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    return announcementBoardReader
        .findActiveBoard(announcementBoardId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private Member validateHrMember(Long userId) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || userId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, userId);
    if (member == null || member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.FORBIDDEN);
    }

    return member;
  }
}
