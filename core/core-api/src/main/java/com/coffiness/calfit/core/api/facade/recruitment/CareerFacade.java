package com.coffiness.calfit.core.api.facade.recruitment;

import com.coffiness.calfit.api.v1.response.CareerApplyFormResponse;
import com.coffiness.calfit.api.v1.response.CareerCompnayResponse;
import com.coffiness.calfit.api.v1.response.CareerRecruitmentResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.OpenRecruitmentInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentReader;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareerFacade {
  private final RecruitmentReader recruitmentReader;
  private final WorkspaceReader workspaceReader;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final ApplicationTemplateRepository applicationTemplateRepository;

  public List<CareerCompnayResponse> getCompanies(String search) {
    Map<String, Long> countMap = recruitmentReader.countOpenRecruitmentsByTenant();
    if (countMap.isEmpty()) {
      return List.of();
    }

    List<Workspace> workspaces = workspaceReader.getAllWorkspaces();

    return workspaces.stream()
        .filter(ws -> countMap.containsKey(ws.workspaceId()))
        .filter(
            ws ->
                search == null
                    || search.isBlank()
                    || ws.name().toLowerCase().contains(search.toLowerCase()))
        .map(
            ws ->
                new CareerCompnayResponse(
                    ws.workspaceId(),
                    ws.name(),
                    ws.employeeScale(),
                    countMap.get(ws.workspaceId())))
        .toList();
  }

  public List<CareerRecruitmentResponse> getRecruitments(String workspaceId, String search) {
    List<OpenRecruitmentInfo> recruitments;
    if (search == null || search.isBlank()) {
      recruitments = recruitmentReader.readOpenByTenantId(workspaceId);
    } else {
      recruitments = recruitmentReader.readOpenByTenantIdAndSearch(workspaceId, search);
    }
    return recruitments.stream().map(CareerRecruitmentResponse::from).toList();
  }

  public CareerApplyFormResponse getApplyForm(String workspaceId, Long recruitmentId) {
    TenantContext.setTenantId(workspaceId);
    try {
      RecruitmentEntity recruitment =
          recruitmentRepository
              .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
              .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

      if (recruitment.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }

      Long templateId = recruitment.getApplicationTemplateId();
      Long firstStageId =
          recruitmentStageRepository.findByRecruitmentIdOrderByStageStepAsc(recruitmentId).stream()
              .findFirst()
              .map(stage -> stage.getId())
              .orElseThrow(() -> new CoreException(ErrorType.VALIDATION_ERROR));

      String customFields =
          applicationTemplateRepository
              .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
              .map(ApplicationTemplateEntity::getFormFields)
              .orElse("[]");

      return new CareerApplyFormResponse(
          recruitmentId, recruitment.getTitle(), templateId, firstStageId, customFields);
    } finally {
      TenantContext.clear();
    }
  }
}
