package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStageType;

public record RecruitmentStage(
    Long id, String stageName, Integer stageStep, RecruitmentStageType stageType) {}
