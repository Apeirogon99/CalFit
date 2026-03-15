package com.coffiness.calfit.domain.interview;

import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.domain.interview.event.InterviewCreatedEvent;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewService {
  private static final int MAX_LOCK_RETRY_ATTEMPTS = 3;
  private static final long LOCK_RETRY_BACKOFF_MILLIS = 120L;

  private final InterviewReader interviewReader;
  private final InterviewStore interviewStore;
  private final InterviewValidator interviewValidator;
  private final DomainEventPublisher domainEventPublisher;

  @Transactional
  public Interview create(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      InterviewRound round,
      List<Long> interviewerIds,
      List<Long> applicantIds,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo) {

    interviewValidator.validateHrMember(userId);
    interviewValidator.validateCreateInput(
        recruitmentId,
        recruitmentStageId,
        round,
        meetingRoomId,
        interviewerIds,
        applicantIds,
        scheduledAt,
        durationMinutes);
    interviewValidator.validateInterviewTime(scheduledAt, durationMinutes);
    interviewValidator.validatePendingApplicants(recruitmentId, recruitmentStageId, applicantIds);

    int totalParticipants = interviewerIds.size() + applicantIds.size();
    interviewValidator.validateCapacity(meetingRoomId, totalParticipants);

    Long scheduleId = null;
    for (int attempt = 1; attempt <= MAX_LOCK_RETRY_ATTEMPTS; attempt++) {
      try {
        scheduleId =
            interviewStore.createConfirmedSchedule(
                userId,
                recruitmentId,
                recruitmentStageId,
                meetingRoomId,
                scheduledAt,
                durationMinutes,
                memo,
                interviewerIds,
                applicantIds);
        break;
      } catch (RuntimeException exception) {
        if (!isLockException(exception) || attempt == MAX_LOCK_RETRY_ATTEMPTS) {
          throw exception;
        }
        sleepBackoff(attempt);
      }
    }

    if (scheduleId == null) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    domainEventPublisher.publish(
        InterviewCreatedEvent.of(
            currentTenantId(),
            scheduleId,
            recruitmentId,
            userId,
            meetingRoomId,
            scheduledAt,
            durationMinutes,
            interviewerIds,
            applicantIds));

    return Interview.confirmed(
        scheduleId,
        recruitmentId,
        recruitmentStageId,
        round,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        memo,
        interviewerIds,
        applicantIds);
  }

  // 면접 생성 화면에 필요한 단계별 대기 지원자 목록을 조회
  @Transactional(readOnly = true)
  public List<PendingInterviewStage> getPendingInterviewStages(Long userId, Long recruitmentId) {
    interviewValidator.validateHrMember(userId);
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return interviewReader.getPendingInterviewStages(recruitmentId);
  }

  @Transactional(readOnly = true)
  public InterviewAvailability getAvailability(
      Long userId,
      LocalDateTime from,
      List<Long> meetingRoomIds,
      List<Long> interviewerIds,
      List<Long> applicantIds) {

    interviewValidator.validateHrMember(userId);
    interviewValidator.validateFromDate(from);

    LocalDateTime to = from.plusMonths(1);

    meetingRoomIds = meetingRoomIds == null ? List.of() : meetingRoomIds;
    interviewerIds = interviewerIds == null ? List.of() : interviewerIds;
    applicantIds = applicantIds == null ? List.of() : applicantIds;

    List<InterviewAvailability.MeetingRoomBusySlot> meetingRoomBusySlots =
        interviewReader.findMeetingRoomBusySlots(meetingRoomIds, from, to);

    List<InterviewAvailability.InterviewerBusySlot> interviewerBusySlots =
        interviewReader.findInterviewerBusySlots(interviewerIds, from, to);

    List<InterviewAvailability.ApplicantBusySlot> applicantBusySlots =
        interviewReader.findApplicantBusySlots(applicantIds, from, to);

    return new InterviewAvailability(
        meetingRoomBusySlots, interviewerBusySlots, applicantBusySlots);
  }

  @Transactional
  public void cancelConfirmedSchedule(Long userId, Long interviewScheduleId) {
    interviewStore.cancelConfirmedSchedule(userId, interviewScheduleId);
  }

  private boolean isLockException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof CannotAcquireLockException
          || current instanceof PessimisticLockingFailureException
          || current instanceof LockTimeoutException
          || current instanceof PessimisticLockException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void sleepBackoff(int attempt) {
    try {
      Thread.sleep(LOCK_RETRY_BACKOFF_MILLIS * attempt);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }
}
