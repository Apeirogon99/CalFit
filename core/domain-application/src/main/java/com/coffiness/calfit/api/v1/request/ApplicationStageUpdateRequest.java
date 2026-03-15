package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotNull;

public record ApplicationStageUpdateRequest(@NotNull Long recruitmentProcessId) {}
