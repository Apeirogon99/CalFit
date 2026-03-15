package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import java.util.List;

public record KanbanColumn(
    Long recruitmentProcessId,
    String name,
    int order,
    RecruitmentStageType stageType,
    List<ApplicationSummaryResponse> applications) {}
