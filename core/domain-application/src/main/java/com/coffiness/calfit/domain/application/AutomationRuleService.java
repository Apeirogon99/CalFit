package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.request.AutomationRuleUpsertRequest;
import com.coffiness.calfit.api.v1.response.AutomationRuleResponse;
import com.coffiness.calfit.api.v1.response.AutomationTemplateResponse;
import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationTemplateCode;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutomationRuleService {

  private static final String DEFAULT_TENANT = "default";

  private final AutomationRuleRepository automationRuleRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public List<AutomationRuleResponse> getRules(Long recruitmentId) {
    requireTenant();
    validateRecruitment(recruitmentId);
    return automationRuleRepository
        .findByRecruitmentIdAndStatusOrderByCreatedAtAsc(recruitmentId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AutomationTemplateResponse> getTemplates() {
    return java.util.Arrays.stream(AutomationTemplateCode.values())
        .map(
            template ->
                new AutomationTemplateResponse(
                    template.name(),
                    template.getLabel(),
                    template.getActionType(),
                    template.getRecommendedStageTypes()))
        .toList();
  }

  @Transactional
  public AutomationRuleResponse createRule(
      Long recruitmentId, AutomationRuleUpsertRequest request) {
    requireTenant();
    validateRequest(recruitmentId, request);
    ensureNoDuplicate(
        recruitmentId,
        request.recruitmentProcessId(),
        request.triggerType(),
        request.actionType(),
        null);

    AutomationRuleEntity saved =
        automationRuleRepository.save(
            AutomationRuleEntity.builder()
                .recruitmentId(recruitmentId)
                .recruitmentProcessId(request.recruitmentProcessId())
                .triggerType(request.triggerType())
                .actionType(request.actionType())
                .payload(toJson(request.payload()))
                .build());
    return toResponse(saved);
  }

  @Transactional
  public AutomationRuleResponse updateRule(Long ruleId, AutomationRuleUpsertRequest request) {
    requireTenant();
    if (ruleId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    AutomationRuleEntity entity =
        automationRuleRepository
            .findByIdAndStatus(ruleId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    validateRequest(entity.getRecruitmentId(), request);
    ensureNoDuplicate(
        entity.getRecruitmentId(),
        request.recruitmentProcessId(),
        request.triggerType(),
        request.actionType(),
        entity.getId());

    entity.update(
        request.recruitmentProcessId(),
        request.triggerType(),
        request.actionType(),
        toJson(request.payload()));
    return toResponse(entity);
  }

  @Transactional
  public void deleteRule(Long ruleId) {
    requireTenant();
    if (ruleId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    AutomationRuleEntity entity =
        automationRuleRepository
            .findByIdAndStatus(ruleId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }

  private void validateRequest(Long recruitmentId, AutomationRuleUpsertRequest request) {
    if (recruitmentId == null || request == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    validateRecruitment(recruitmentId);
    if (!recruitmentStageRepository.existsByIdAndRecruitmentId(
        request.recruitmentProcessId(), recruitmentId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    validatePayload(request.actionType(), request.payload());
  }

  private void validatePayload(AutomationActionType actionType, JsonNode payload) {
    JsonNode normalizedPayload = normalizePayload(payload);
    if (actionType == AutomationActionType.EMAIL) {
      JsonNode templateCodeNode =
          normalizedPayload == null ? null : normalizedPayload.get("templateCode");
      if (templateCodeNode == null
          || !templateCodeNode.isTextual()
          || !AutomationTemplateCode.supports(templateCodeNode.asText())) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
    }
  }

  private void validateRecruitment(Long recruitmentId) {
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    recruitmentRepository
        .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private void ensureNoDuplicate(
      Long recruitmentId,
      Long recruitmentProcessId,
      com.coffiness.calfit.core.enums.AutomationTriggerType triggerType,
      com.coffiness.calfit.core.enums.AutomationActionType actionType,
      Long currentRuleId) {
    List<AutomationRuleEntity> rules =
        automationRuleRepository.findByRecruitmentIdAndStatusOrderByCreatedAtAsc(
            recruitmentId, EntityStatus.ACTIVE);
    boolean duplicated =
        rules.stream()
            .anyMatch(
                rule ->
                    !rule.getId().equals(currentRuleId)
                        && rule.getRecruitmentProcessId().equals(recruitmentProcessId)
                        && rule.getTriggerType() == triggerType
                        && rule.getActionType() == actionType);
    if (duplicated) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private AutomationRuleResponse toResponse(AutomationRuleEntity entity) {
    return new AutomationRuleResponse(
        entity.getId(),
        entity.getRecruitmentId(),
        entity.getRecruitmentProcessId(),
        entity.getTriggerType(),
        entity.getActionType(),
        parsePayload(entity.getPayload()));
  }

  private JsonNode parsePayload(String payload) {
    try {
      return payload == null
          ? objectMapper.nullNode()
          : normalizePayload(objectMapper.readTree(payload));
    } catch (JsonProcessingException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private String toJson(JsonNode payload) {
    try {
      return objectMapper.writeValueAsString(normalizePayload(payload));
    } catch (JsonProcessingException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private JsonNode normalizePayload(JsonNode payload) {
    if (payload == null || payload.isNull()) {
      return objectMapper.nullNode();
    }
    if (!payload.isTextual()) {
      return payload;
    }
    String raw = payload.asText();
    if (raw == null || raw.isBlank()) {
      return objectMapper.nullNode();
    }
    try {
      return objectMapper.readTree(raw);
    } catch (JsonProcessingException e) {
      return payload;
    }
  }

  private void requireTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
