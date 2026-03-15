package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.ApplicationTemplateListResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationTemplateController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
  private static final String STATUS_IN_USE = "사용중";
  private static final String STATUS_UNUSED = "미사용";

  private final ApplicationTemplateRepository applicationTemplateRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final ObjectMapper objectMapper;
  private final MemberReader memberReader;

  @GetMapping("/api/v1/application-templates")
  public ApiResponse<List<ApplicationTemplateListResponse>> getApplicationTemplates() {
    List<ApplicationTemplateEntity> templates = applicationTemplateRepository.findActiveTemplates();
    Map<Long, Long> recruitmentCountByTemplateId =
        getRecruitmentCountByTemplateId(
            templates.stream().map(ApplicationTemplateEntity::getId).toList());

    List<ApplicationTemplateListResponse> response =
        templates.stream()
            .map(template -> toListResponse(template, recruitmentCountByTemplateId))
            .toList();

    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/workspaces/{workspaceId}/application-templates")
  public ApiResponse<List<ApplicationTemplateListResponse>> getApplicationTemplatesByWorkspace(
      @PathVariable String workspaceId) {
    return getApplicationTemplates();
  }

  @PostMapping("/api/v1/application-templates")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> createApplicationTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @RequestBody ApplicationTemplateWriteRequest request) {
    validateHrUser(user);

    String templateName = request.resolveTemplateName();
    if (!StringUtils.hasText(templateName)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    String formFields = request.toFormFieldsJson(objectMapper);
    boolean inUse = request.resolveRequestedUseStatus().orElse(false);

    ApplicationTemplateEntity saved =
        applicationTemplateRepository.save(
            ApplicationTemplateEntity.create(templateName, formFields, inUse));

    return ApiResponse.success(toListResponse(saved, 0L));
  }

  @PostMapping("/api/v1/workspaces/{workspaceId}/application-templates")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> createApplicationTemplateByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @RequestBody ApplicationTemplateWriteRequest request) {
    return createApplicationTemplate(user, request);
  }

  @PutMapping("/api/v1/application-templates/{templateId}")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> updateApplicationTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long templateId,
      @RequestBody ApplicationTemplateWriteRequest request) {
    validateHrUser(user);

    ApplicationTemplateEntity template =
        applicationTemplateRepository
            .findActiveById(templateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    String templateName = request.resolveTemplateName();
    if (!StringUtils.hasText(templateName)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    template.updateTemplate(templateName, request.toFormFieldsJson(objectMapper));
    request.resolveRequestedUseStatus().ifPresent(template::updateUseStatus);

    Long recruitmentCount =
        getRecruitmentCountByTemplateId(List.of(templateId)).getOrDefault(templateId, 0L);
    return ApiResponse.success(toListResponse(template, recruitmentCount));
  }

  @PutMapping("/api/v1/workspaces/{workspaceId}/application-templates/{templateId}")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> updateApplicationTemplateByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @PathVariable Long templateId,
      @RequestBody ApplicationTemplateWriteRequest request) {
    return updateApplicationTemplate(user, templateId, request);
  }

  @PatchMapping("/api/v1/application-templates/{templateId}/status")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> updateApplicationTemplateStatus(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long templateId,
      @RequestBody ApplicationTemplateStatusRequest request) {
    validateHrUser(user);

    ApplicationTemplateEntity template =
        applicationTemplateRepository
            .findActiveById(templateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    boolean inUse =
        request
            .resolveRequestedUseStatus()
            .orElseThrow(() -> new CoreException(ErrorType.VALIDATION_ERROR));
    template.updateUseStatus(inUse);

    Long recruitmentCount =
        getRecruitmentCountByTemplateId(List.of(templateId)).getOrDefault(templateId, 0L);
    return ApiResponse.success(toListResponse(template, recruitmentCount));
  }

  @PatchMapping("/api/v1/workspaces/{workspaceId}/application-templates/{templateId}/status")
  @Transactional
  public ApiResponse<ApplicationTemplateListResponse> updateApplicationTemplateStatusByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @PathVariable Long templateId,
      @RequestBody ApplicationTemplateStatusRequest request) {
    return updateApplicationTemplateStatus(user, templateId, request);
  }

  @DeleteMapping("/api/v1/application-templates/{templateId}")
  @Transactional
  public ApiResponse<Void> deleteApplicationTemplate(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long templateId) {
    validateHrUser(user);

    ApplicationTemplateEntity template =
        applicationTemplateRepository
            .findActiveById(templateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    Long recruitmentCount =
        getRecruitmentCountByTemplateId(List.of(templateId)).getOrDefault(templateId, 0L);
    if (recruitmentCount > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    template.deleted();
    return ApiResponse.success(null);
  }

  @DeleteMapping("/api/v1/workspaces/{workspaceId}/application-templates/{templateId}")
  @Transactional
  public ApiResponse<Void> deleteApplicationTemplateByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @PathVariable Long templateId) {
    return deleteApplicationTemplate(user, templateId);
  }

  private void validateHrUser(SecurityUser user) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, user.userId());
    if (member == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.FORBIDDEN);
    }
  }

  private Map<Long, Long> getRecruitmentCountByTemplateId(List<Long> templateIds) {
    if (templateIds == null || templateIds.isEmpty()) {
      return Map.of();
    }
    List<Object[]> rows =
        recruitmentRepository.countByTemplateIds(templateIds, EntityStatus.ACTIVE);
    return rows.stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
  }

  private ApplicationTemplateListResponse toListResponse(
      ApplicationTemplateEntity template, Map<Long, Long> recruitmentCountByTemplateId) {
    Long recruitmentCount = recruitmentCountByTemplateId.getOrDefault(template.getId(), 0L);
    return toListResponse(template, recruitmentCount);
  }

  private ApplicationTemplateListResponse toListResponse(
      ApplicationTemplateEntity template, Long recruitmentCount) {
    boolean used = template.isInUse();
    return new ApplicationTemplateListResponse(
        template.getId(),
        template.getName(),
        template.getCreatedAt().format(DATE_FORMATTER),
        template.getUpdatedAt().format(DATE_FORMATTER),
        used ? STATUS_IN_USE : STATUS_UNUSED,
        used,
        recruitmentCount == null ? 0L : recruitmentCount,
        template.getFormFields());
  }

  private static Optional<Boolean> resolveUseStatus(Boolean used, String... rawStatuses) {
    if (used != null) {
      return Optional.of(used);
    }

    for (String rawStatus : rawStatuses) {
      if (!StringUtils.hasText(rawStatus)) {
        continue;
      }

      String compact = rawStatus.replaceAll("\\s+", "");
      if (STATUS_IN_USE.equals(compact)
          || "IN_USE".equalsIgnoreCase(compact)
          || "USED".equalsIgnoreCase(compact)
          || "ACTIVE".equalsIgnoreCase(compact)) {
        return Optional.of(true);
      }

      if (STATUS_UNUSED.equals(compact)
          || "UNUSED".equalsIgnoreCase(compact)
          || "INACTIVE".equalsIgnoreCase(compact)) {
        return Optional.of(false);
      }
    }

    return Optional.empty();
  }

  private record ApplicationTemplateWriteRequest(
      String title,
      String name,
      String templateName,
      Object customFields,
      Object questions,
      Object questionFields,
      Object customQuestions,
      Object schema,
      String formFields,
      String status,
      String templateStatus,
      String useStatus,
      Boolean used,
      Boolean isDefault) {

    private String resolveTemplateName() {
      if (StringUtils.hasText(title)) {
        return title.trim();
      }
      if (StringUtils.hasText(name)) {
        return name.trim();
      }
      return StringUtils.hasText(templateName) ? templateName.trim() : "";
    }

    private String toFormFieldsJson(ObjectMapper objectMapper) {
      Object schemaSource =
          firstNonNull(
              customFields, questions, questionFields, customQuestions, formFields, schema);
      if (schemaSource == null) {
        schemaSource = List.of();
      }

      try {
        Object normalizedSchema = normalizeSchemaSource(schemaSource, objectMapper);
        return objectMapper.writeValueAsString(normalizedSchema);
      } catch (JsonProcessingException e) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
    }

    private Optional<Boolean> resolveRequestedUseStatus() {
      Boolean requested = used != null ? used : isDefault;
      return ApplicationTemplateController.resolveUseStatus(
          requested, status, templateStatus, useStatus);
    }

    private static Object firstNonNull(Object... values) {
      for (Object value : values) {
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private static Object normalizeSchemaSource(Object schemaSource, ObjectMapper objectMapper)
        throws JsonProcessingException {
      if (schemaSource instanceof Map<?, ?> schemaMap) {
        Object nestedSchema =
            firstNonNull(
                schemaMap.get("customFields"),
                schemaMap.get("questions"),
                schemaMap.get("questionFields"),
                schemaMap.get("customQuestions"),
                schemaMap.get("formFields"),
                schemaMap.get("schema"),
                schemaMap.get("fields"),
                schemaMap.get("items"));
        return nestedSchema == null ? schemaMap : normalizeSchemaSource(nestedSchema, objectMapper);
      }

      if (!(schemaSource instanceof String rawSchema)) {
        return schemaSource;
      }

      if (!StringUtils.hasText(rawSchema)) {
        return List.of();
      }

      String trimmed = rawSchema.trim();
      if (trimmed.startsWith("[")) {
        return objectMapper.readValue(trimmed, List.class);
      }
      if (trimmed.startsWith("{")) {
        return normalizeSchemaSource(
            objectMapper.readValue(trimmed, LinkedHashMap.class), objectMapper);
      }

      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private record ApplicationTemplateStatusRequest(
      String status, String templateStatus, String useStatus, Boolean used, Boolean isDefault) {

    private Optional<Boolean> resolveRequestedUseStatus() {
      Boolean requested = used != null ? used : isDefault;
      return ApplicationTemplateController.resolveUseStatus(
          requested, status, templateStatus, useStatus);
    }
  }
}
