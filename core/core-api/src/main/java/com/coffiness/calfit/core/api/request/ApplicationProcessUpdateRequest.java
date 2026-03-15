package com.coffiness.calfit.core.api.request;

import jakarta.validation.constraints.NotNull;

public record ApplicationProcessUpdateRequest(@NotNull Long recruitmentProcessId) {}
