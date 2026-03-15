package com.coffiness.calfit.domain.interview;

import com.coffiness.calfit.core.enums.InterviewRound;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewValidator {

  void validateHrMember(Long userId);

  void validateCreateInput(
      Long recruitmentId,
      Long recruitmentStageId,
      InterviewRound round,
      Long meetingRoomId,
      List<Long> interviewerIds,
      List<Long> applicantIds,
      LocalDateTime scheduledAt,
      Integer durationMinutes);

  void validateInterviewTime(LocalDateTime scheduledAt, Integer durationMinutes);

  void validatePendingApplicants(
      Long recruitmentId, Long recruitmentStageId, List<Long> applicantIds);

  void validateCapacity(Long meetingRoomId, int totalParticipants);

  void validateFromDate(LocalDateTime from);
}
