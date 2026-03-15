package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.RecruitmentStageType;

public record RecruitmentStageRequest(
    String stageName, RecruitmentStageType stageType, Integer stageStep) {}
