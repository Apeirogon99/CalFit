package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.CostCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCostRequest(
    @NotNull CostCategory category,
    @NotBlank String name,
    @NotNull @Min(1) Long amount,
    @NotNull Integer year,
    @NotNull Integer month,
    String memo) {}
