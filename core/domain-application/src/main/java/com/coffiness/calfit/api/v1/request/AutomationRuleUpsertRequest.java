package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record AutomationRuleUpsertRequest(
    @NotNull Long recruitmentProcessId,
    @NotNull AutomationTriggerType triggerType,
    @NotNull AutomationActionType actionType,
    @NotNull JsonNode payload) {}
