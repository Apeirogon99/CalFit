package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationEventStatus;
import com.coffiness.calfit.core.enums.AutomationTemplateCode;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.support.email.ApplicantEmailMessage;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutomationEventExecutor {

  private static final String TEMPLATE_CODE_FIELD = "templateCode";

  private final AutomationEventRepository automationEventRepository;
  private final AutomationRuleRepository automationRuleRepository;
  private final ApplicationRepository applicationRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final EmailService emailService;
  private final EmailProperties emailProperties;
  private final ApplicantAutomationEmailFactory applicantAutomationEmailFactory;
  private final ObjectMapper objectMapper;

  @Transactional
  public void executePendingForApplication(Long applicationId) {
    if (applicationId == null) {
      return;
    }
    List<AutomationEventEntity> events =
        automationEventRepository.findByApplicationIdAndEventStatus(
            applicationId, AutomationEventStatus.PENDING);
    for (AutomationEventEntity event : events) {
      handleEvent(event);
    }
  }

  private void handleEvent(AutomationEventEntity event) {
    AutomationActionType actionType = event.getActionType();
    try {
      switch (actionType) {
        case EMAIL -> handleEmailEvent(event);
        case SLACK -> event.markFailed("SLACK_NOT_ENABLED");
        default -> event.markFailed("UNKNOWN_ACTION");
      }
    } catch (RuntimeException e) {
      event.markFailed(resolveErrorMessage(e));
    }
  }

  private void handleEmailEvent(AutomationEventEntity event) {
    AutomationRuleEntity rule =
        automationRuleRepository
            .findByIdAndStatus(event.getRuleId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("AUTOMATION_RULE_NOT_FOUND"));
    ApplicationEntity application =
        applicationRepository
            .findByIdAndStatus(event.getApplicationId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("APPLICATION_NOT_FOUND"));
    RecruitmentEntity recruitment =
        recruitmentRepository
            .findByIdAndStatus(application.getRecruitmentId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("RECRUITMENT_NOT_FOUND"));
    RecruitmentStageEntity destinationStage =
        recruitmentStageRepository
            .findById(rule.getRecruitmentProcessId())
            .orElseThrow(() -> new IllegalStateException("RECRUITMENT_STAGE_NOT_FOUND"));

    AutomationTemplateCode templateCode = extractTemplateCode(rule);
    ApplicantEmailMessage message =
        applicantAutomationEmailFactory.create(
            templateCode, application, recruitment, destinationStage);

    emailService.sendApplicantResultEmail(emailProperties.getSender(), message);
    event.markSuccess();
  }

  private AutomationTemplateCode extractTemplateCode(AutomationRuleEntity rule) {
    try {
      JsonNode payload = normalizePayload(objectMapper.readTree(rule.getPayload()));
      JsonNode templateCodeNode = payload == null ? null : payload.get(TEMPLATE_CODE_FIELD);
      if (templateCodeNode == null || !templateCodeNode.isTextual()) {
        throw new IllegalStateException("AUTOMATION_TEMPLATE_CODE_MISSING");
      }
      return AutomationTemplateCode.valueOf(templateCodeNode.asText());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("AUTOMATION_TEMPLATE_CODE_INVALID", e);
    } catch (Exception e) {
      throw new IllegalStateException("AUTOMATION_PAYLOAD_INVALID", e);
    }
  }

  private JsonNode normalizePayload(JsonNode payload) {
    if (payload == null || payload.isNull() || !payload.isTextual()) {
      return payload;
    }
    String raw = payload.asText();
    if (raw == null || raw.isBlank()) {
      return payload;
    }
    try {
      return objectMapper.readTree(raw);
    } catch (Exception e) {
      return payload;
    }
  }

  private String resolveErrorMessage(RuntimeException e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return "AUTOMATION_EXECUTION_FAILED";
    }
    return message.length() > 1000 ? message.substring(0, 1000) : message;
  }
}
