package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.core.enums.AutomationTemplateCode;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.email.ApplicantEmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicantAutomationEmailFactory {

  private static final String DEFAULT_COMPANY_NAME = "CalFit";

  private final RecruitmentRepository recruitmentRepository;
  private final WorkspaceRepository workspaceRepository;

  public ApplicantEmailMessage create(
      AutomationTemplateCode templateCode,
      ApplicationEntity application,
      RecruitmentEntity recruitment,
      RecruitmentStageEntity destinationStage) {
    String companyName = resolveCompanyName(recruitment.getId());
    String positionName = recruitment.getTitle();

    return switch (templateCode) {
      case DOCUMENT_PASS ->
          ApplicantEmailMessage.builder()
              .applicantEmail(application.getEmail())
              .applicantName(application.getName())
              .companyName(companyName)
              .positionName(positionName)
              .accepted(true)
              .subject(String.format("[%s] %s 서류 전형 합격 안내", companyName, positionName))
              .headline("서류 전형 합격을 축하드립니다")
              .summary("서류 검토가 완료되었고 다음 전형으로 이동되었습니다. 이후 일정은 별도로 안내드릴 예정입니다.")
              .highlightLabel("다음 전형")
              .highlightValue(destinationStage.getStageName())
              .build();
      case INTERVIEW_REQUEST ->
          ApplicantEmailMessage.builder()
              .applicantEmail(application.getEmail())
              .applicantName(application.getName())
              .companyName(companyName)
              .positionName(positionName)
              .accepted(true)
              .subject(String.format("[%s] %s 면접 안내", companyName, positionName))
              .headline("면접 전형이 시작되었습니다")
              .summary("면접 전형이 진행될 예정입니다. 상세 일정과 준비 사항은 별도로 안내드릴 예정입니다.")
              .highlightLabel("현재 단계")
              .highlightValue(destinationStage.getStageName())
              .build();
      case FINAL_PASS ->
          ApplicantEmailMessage.builder()
              .applicantEmail(application.getEmail())
              .applicantName(application.getName())
              .companyName(companyName)
              .positionName(positionName)
              .accepted(true)
              .subject(String.format("[%s] %s 최종 합격 안내", companyName, positionName))
              .headline("최종 합격을 축하드립니다")
              .summary("모든 전형을 통과하셨습니다. 이후 입사 절차는 담당자가 별도로 안내드릴 예정입니다.")
              .highlightLabel("현재 상태")
              .highlightValue(destinationStage.getStageName())
              .build();
      case FAIL_NOTIFICATION ->
          ApplicantEmailMessage.builder()
              .applicantEmail(application.getEmail())
              .applicantName(application.getName())
              .companyName(companyName)
              .positionName(positionName)
              .accepted(false)
              .subject(String.format("[%s] %s 전형 결과 안내", companyName, positionName))
              .headline("전형 결과를 안내드립니다")
              .summary("신중한 검토 끝에 이번 전형에서는 다음 단계로 함께하지 못하게 되었습니다.")
              .highlightLabel("최종 검토 단계")
              .highlightValue(destinationStage.getStageName())
              .build();
    };
  }

  private String resolveCompanyName(Long recruitmentId) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
      tenantId = recruitmentRepository.findTenantIdById(recruitmentId).orElse(null);
    }
    if (tenantId == null || tenantId.isBlank()) {
      return DEFAULT_COMPANY_NAME;
    }
    return workspaceRepository
        .findByWorkspaceId(tenantId)
        .map(workspace -> workspace.getName())
        .orElse(DEFAULT_COMPANY_NAME);
  }
}
