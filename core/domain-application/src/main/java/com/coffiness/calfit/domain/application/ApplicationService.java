package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationStageUpdateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationAnswerResponse;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationFileResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.enums.AutomationEventStatus;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryEntity;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  private static final String DEFAULT_TENANT = "default";
  private final ApplicationRepository applicationRepository;
  private final ApplicationFileRepository applicationFileRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final ApplicationTemplateRepository applicationTemplateRepository;
  private final ApplicationProcessHistoryRepository applicationProcessHistoryRepository;
  private final AutomationRuleRepository automationRuleRepository;
  private final AutomationEventRepository automationEventRepository;
  private final AutomationEventExecutor automationEventExecutor;
  private final ObjectMapper objectMapper;

  @Transactional
  public Long create(ApplicationCreateRequest request, Long requesterUserId) {
    ensureTenantFromRecruitment(request.recruitmentId());

    Long applicantId = request.applicantId() != null ? request.applicantId() : requesterUserId;
    if (requesterUserId == null || !requesterUserId.equals(applicantId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (applicationRepository.existsByApplicantIdAndRecruitmentId(
        applicantId, request.recruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    RecruitmentEntity recruitment = getOpenRecruitment(request.recruitmentId());
    Long templateId = resolveTemplateId(request.templateId(), recruitment);
    Long recruitmentProcessId =
        resolveRecruitmentProcessId(request.recruitmentProcessId(), request.recruitmentId());

    LocalDateTime birthDate = request.birthDate().atStartOfDay();
    ApplicationEntity entity =
        ApplicationEntity.create(
            applicantId,
            request.recruitmentId(),
            recruitmentProcessId,
            templateId,
            request.name(),
            request.gender(),
            birthDate,
            request.phone(),
            request.email(),
            request.formFields().toString());

    ApplicationEntity saved = applicationRepository.save(entity);
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public List<ApplicationSummaryResponse> listByRecruitment(Long recruitmentId) {
    requireTenant();
    List<ApplicationEntity> entities =
        applicationRepository.findByRecruitmentIdAndStatus(recruitmentId, EntityStatus.ACTIVE);
    return entities.stream().map(ApplicationSummaryResponse::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ApplicationSummaryResponse> listAll() {
    requireTenant();
    List<ApplicationEntity> entities = applicationRepository.findByStatus(EntityStatus.ACTIVE);
    return entities.stream().map(ApplicationSummaryResponse::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ApplicationDetailResponse getDetail(Long applicationId) {
    requireTenant();
    ApplicationEntity entity = getActiveApplication(applicationId);
    return buildDetailResponse(entity);
  }

  @Transactional
  public ApplicationDetailResponse updateStage(
      Long applicationId, ApplicationStageUpdateRequest request, Long actorId) {
    requireTenant();
    updateProcess(applicationId, request.recruitmentProcessId(), actorId);
    return buildDetailResponse(getActiveApplication(applicationId));
  }

  private RecruitmentEntity getOpenRecruitment(Long recruitmentId) {
    RecruitmentEntity recruitment = getRecruitment(recruitmentId);

    if (recruitment.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    return recruitment;
  }

  private RecruitmentEntity getRecruitment(Long recruitmentId) {
    return recruitmentRepository
        .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private ApplicationEntity getActiveApplication(Long applicationId) {
    return applicationRepository
        .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private RecruitmentStageEntity getRecruitmentStage(Long stageId) {
    return recruitmentStageRepository
        .findById(stageId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private ApplicationDetailResponse buildDetailResponse(ApplicationEntity entity) {
    List<ApplicationFileEntity> files =
        applicationFileRepository.findByApplicationIdAndStatusAndUploadStatus(
            entity.getId(), EntityStatus.ACTIVE, UploadStatus.ACTIVE);
    List<ApplicationFileResponse> fileResponses =
        files.stream().map(ApplicationFileResponse::from).collect(Collectors.toList());
    String shortBio = extractShortBio(entity.getFormFields());
    List<ApplicationAnswerResponse> answers =
        extractAnswerResponses(entity.getTemplateId(), entity.getFormFields());
    return ApplicationDetailResponse.from(entity, shortBio, answers, fileResponses);
  }

  // 지원서 자기소개 추출
  private String extractShortBio(String rawFormFields) {
    JsonNode formFieldsNode = readJsonNode(rawFormFields);
    if (formFieldsNode == null || !formFieldsNode.isObject()) {
      return null;
    }

    return normalizeAnswerValue(formFieldsNode.get("shortBio"));
  }

  // 지원서 답변 목록 추출
  private List<ApplicationAnswerResponse> extractAnswerResponses(
      Long templateId, String rawFormFields) {
    JsonNode formFieldsNode = readJsonNode(rawFormFields);
    if (formFieldsNode == null || !formFieldsNode.isObject()) {
      return List.of();
    }

    Map<String, String> labelMap = readAnswerLabelMap(templateId);
    List<ApplicationAnswerResponse> answers = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> fields = formFieldsNode.fields();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String key = field.getKey();
      if ("shortBio".equalsIgnoreCase(key)) {
        continue;
      }

      String value = normalizeAnswerValue(field.getValue());
      if (!StringUtils.hasText(value)) {
        continue;
      }

      String label = resolveAnswerLabel(key, labelMap, answers.size() + 1);
      answers.add(new ApplicationAnswerResponse(key, label, value));
    }

    return answers;
  }

  // 지원서 라벨 맵 조회
  private Map<String, String> readAnswerLabelMap(Long templateId) {
    if (templateId == null) {
      return Map.of();
    }

    return applicationTemplateRepository
        .findFormFieldsById(templateId)
        .map(this::parseAnswerLabelMap)
        .orElseGet(Map::of);
  }

  // 템플릿 필드 라벨 맵 파싱
  private Map<String, String> parseAnswerLabelMap(String rawTemplateFields) {
    JsonNode templateFieldsNode = readJsonNode(rawTemplateFields);
    if (templateFieldsNode == null) {
      return Map.of();
    }

    JsonNode targetNode = templateFieldsNode;
    if (templateFieldsNode.isObject()) {
      targetNode =
          firstNonNull(
              templateFieldsNode.get("customFields"),
              templateFieldsNode.get("questions"),
              templateFieldsNode.get("questionFields"),
              templateFieldsNode.get("customQuestions"),
              templateFieldsNode.get("formFields"),
              templateFieldsNode.get("schema"),
              templateFieldsNode.get("fields"),
              templateFieldsNode.get("items"));
    }

    if (targetNode == null || !targetNode.isArray()) {
      return Map.of();
    }

    Map<String, String> labelMap = new LinkedHashMap<>();
    for (JsonNode fieldNode : targetNode) {
      String key = textValue(fieldNode.get("id"));
      String label = textValue(firstNonNull(fieldNode.get("label"), fieldNode.get("question")));
      if (StringUtils.hasText(key) && StringUtils.hasText(label)) {
        labelMap.put(key, label);
      }
    }

    return labelMap;
  }

  // 답변 라벨 결정
  private String resolveAnswerLabel(String key, Map<String, String> labelMap, int fallbackIndex) {
    String label = labelMap.get(key);
    if (StringUtils.hasText(label)) {
      return label;
    }

    if ("portfolioUrl".equalsIgnoreCase(key) || "portfolio".equalsIgnoreCase(key)) {
      return "포트폴리오 URL";
    }

    if (key != null && key.startsWith("custom_")) {
      return "추가 질문 " + fallbackIndex;
    }

    return key;
  }

  // 답변 값 문자열 변환
  private String normalizeAnswerValue(JsonNode valueNode) {
    if (valueNode == null || valueNode.isNull()) {
      return null;
    }

    if (valueNode.isTextual()) {
      return normalizeRawTextAnswer(valueNode.asText());
    }

    if (valueNode.isArray()) {
      List<String> values = new ArrayList<>();
      for (JsonNode itemNode : valueNode) {
        String normalized = normalizeAnswerValue(itemNode);
        if (StringUtils.hasText(normalized)) {
          values.add(normalized);
        }
      }
      return values.isEmpty() ? null : String.join(", ", values);
    }

    if (valueNode.isBoolean()) {
      return valueNode.asBoolean() ? "예" : "아니오";
    }

    if (valueNode.isNumber()) {
      return valueNode.asText();
    }

    return valueNode.toString();
  }

  // 문자열 답변 정규화
  private String normalizeRawTextAnswer(String rawValue) {
    if (!StringUtils.hasText(rawValue)) {
      return null;
    }

    String trimmed = rawValue.trim();
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      JsonNode nestedNode = readJsonNode(trimmed);
      if (nestedNode != null) {
        return normalizeAnswerValue(nestedNode);
      }
    }

    return trimmed;
  }

  // JSON 노드 파싱
  private JsonNode readJsonNode(String rawJson) {
    if (!StringUtils.hasText(rawJson)) {
      return null;
    }

    try {
      JsonNode parsedNode = objectMapper.readTree(rawJson);

      for (int i = 0; i < 3; i += 1) {
        if (parsedNode == null || !parsedNode.isTextual()) {
          break;
        }

        String nestedJson = parsedNode.asText();
        if (!StringUtils.hasText(nestedJson)) {
          break;
        }

        String trimmed = nestedJson.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
          break;
        }

        parsedNode = objectMapper.readTree(trimmed);
      }

      return parsedNode;
    } catch (Exception exception) {
      return null;
    }
  }

  // 첫 번째 유효 노드 조회
  private JsonNode firstNonNull(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (node != null && !node.isNull()) {
        return node;
      }
    }
    return null;
  }

  // 텍스트 값 추출
  private String textValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.asText();
  }

  private Long resolveTemplateId(Long requestTemplateId, RecruitmentEntity recruitment) {
    Long recruitmentTemplateId = recruitment.getApplicationTemplateId();
    if (requestTemplateId == null) {
      return recruitmentTemplateId;
    }
    if (!recruitmentTemplateId.equals(requestTemplateId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return requestTemplateId;
  }

  private Long resolveRecruitmentProcessId(Long requestProcessId, Long recruitmentId) {
    if (requestProcessId == null) {
      return recruitmentStageRepository
          .findByRecruitmentIdOrderByStageStepAsc(recruitmentId)
          .stream()
          .findFirst()
          .map(stage -> stage.getId())
          .orElseThrow(() -> new CoreException(ErrorType.VALIDATION_ERROR));
    }

    if (!recruitmentStageRepository.existsByIdAndRecruitmentId(requestProcessId, recruitmentId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    return requestProcessId;
  }

  @Transactional(readOnly = true)
  public KanbanBoard getKanbanBoard(Long recruitmentId) {
    requireTenant();
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    List<RecruitmentStageEntity> stages =
        recruitmentStageRepository.findByRecruitmentId(recruitmentId);
    if (stages.isEmpty()) {
      return new KanbanBoard(List.of());
    }

    List<KanbanColumn> columns =
        stages.stream()
            .sorted((a, b) -> a.getStageStep().compareTo(b.getStageStep()))
            .map(
                stage -> {
                  List<ApplicationEntity> applications =
                      applicationRepository.findByRecruitmentIdAndRecruitmentProcessIdAndStatus(
                          recruitmentId, stage.getId(), EntityStatus.ACTIVE);
                  List<ApplicationSummaryResponse> summaries =
                      applications.stream().map(ApplicationSummaryResponse::from).toList();
                  return new KanbanColumn(
                      stage.getId(),
                      stage.getStageName(),
                      stage.getStageStep(),
                      stage.getStageType(),
                      summaries);
                })
            .toList();

    return new KanbanBoard(columns);
  }

  @Transactional
  public void updateProcess(Long applicationId, Long recruitmentProcessId, Long actorId) {
    requireTenant();
    if (applicationId == null || recruitmentProcessId == null || actorId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    ApplicationEntity entity =
        applicationRepository
            .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    RecruitmentStageEntity destinationStage = getRecruitmentStage(recruitmentProcessId);
    if (!entity.getRecruitmentId().equals(destinationStage.getRecruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    if (recruitmentProcessId.equals(entity.getRecruitmentProcessId())) {
      return;
    }

    Long beforeProcessId = entity.getRecruitmentProcessId();
    entity.updateRecruitmentProcessId(recruitmentProcessId);

    applicationProcessHistoryRepository.save(
        ApplicationProcessHistoryEntity.builder()
            .applicationId(applicationId)
            .fromRecruitmentProcessId(beforeProcessId)
            .toRecruitmentProcessId(recruitmentProcessId)
            .actorId(actorId)
            .build());

    List<AutomationRuleEntity> rules =
        automationRuleRepository.findByRecruitmentIdAndRecruitmentProcessIdAndTriggerType(
            entity.getRecruitmentId(), recruitmentProcessId, AutomationTriggerType.ON_ENTER);
    for (AutomationRuleEntity rule : rules) {
      automationEventRepository.save(
          AutomationEventEntity.builder()
              .applicationId(applicationId)
              .ruleId(rule.getId())
              .actionType(rule.getActionType())
              .eventStatus(AutomationEventStatus.PENDING)
              .build());
    }

    automationEventExecutor.executePendingForApplication(applicationId);
  }

  private void requireTenant() {
    String tenantId = currentTenantId();
    if (DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }

  private void ensureTenantFromRecruitment(Long recruitmentId) {
    String currentTenantId = TenantContext.getTenantId();
    if (currentTenantId != null
        && !currentTenantId.isBlank()
        && !DEFAULT_TENANT.equals(currentTenantId)) {
      return;
    }
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    String tenantId =
        recruitmentRepository
            .findTenantIdById(recruitmentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    TenantContext.setTenantId(tenantId);
  }
}
