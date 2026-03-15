package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.fasterxml.jackson.databind.JsonNode;

public record AutomationRuleResponse(
    Long ruleId,
    Long recruitmentId,
    Long recruitmentProcessId,
    AutomationTriggerType triggerType,
    AutomationActionType actionType,
    JsonNode payload) {}
