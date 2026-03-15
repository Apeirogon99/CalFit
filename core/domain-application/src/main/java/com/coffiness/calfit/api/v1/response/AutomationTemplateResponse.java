package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import java.util.List;

public record AutomationTemplateResponse(
    String code,
    String label,
    AutomationActionType actionType,
    List<RecruitmentStageType> recommendedStageTypes) {}
