package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.InterviewRound;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record InterviewCreateRequest(
    @NotNull Long recruitmentId,
    @NotNull Long recruitmentStageId,
    @NotNull InterviewRound round,
    @NotEmpty List<Long> interviewerIds,
    @NotEmpty List<Long> applicantIds,
    @NotNull Long meetingRoomId,
    @NotNull LocalDateTime scheduledAt,
    @NotNull Integer durationMinutes,
    String memo) {}
