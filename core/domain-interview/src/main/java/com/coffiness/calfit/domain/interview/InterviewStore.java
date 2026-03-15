package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewStore {

  Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds);

  void cancelConfirmedSchedule(Long userId, Long interviewScheduleId);
}
