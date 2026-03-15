package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.interview.InterviewStore;
import com.coffiness.calfit.storage.db.core.interview.InterviewRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterviewStoreImpl implements InterviewStore {

  private final InterviewRepository interviewRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds) {
    return interviewRepository.createConfirmedSchedule(
        userId,
        recruitmentId,
        recruitmentStageId,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        memo,
        interviewerIds,
        applicantIds);
  }

  @Override
  public void cancelConfirmedSchedule(Long userId, Long interviewScheduleId) {
    interviewRepository.cancelConfirmedSchedule(userId, interviewScheduleId);
  }
}
