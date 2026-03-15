package com.coffiness.calfit.api.applications.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.ApplicationStageUpdateRequest;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
public class PATCH_specs {

  @Test
  void document_stage_move_updates_stage_and_stores_process_history(
      @Autowired TestRestTemplate restTemplate,
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentRepository recruitmentRepository,
      @Autowired RecruitmentStageRepository recruitmentStageRepository,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationProcessHistoryRepository applicationProcessHistoryRepository) {
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long applicationId;
    Long nextStageId;
    TenantContext.setTenantId(tenantId);
    try {
      RecruitmentEntity recruitment =
          recruitmentRepository.save(
              RecruitmentEntity.builder()
                  .creatorId(1L)
                  .title("백엔드 채용")
                  .recruitmentStatus(RecruitmentStatus.OPEN)
                  .targetCount(1)
                  .startDate(LocalDateTime.now().minusDays(1))
                  .endDate(LocalDateTime.now().plusDays(7))
                  .applicationTemplateId(1L)
                  .contents("채용 테스트 공고")
                  .careerType(CareerType.NEW)
                  .leadGroupId(1L)
                  .build());

      RecruitmentStageEntity documentStage =
          recruitmentStageRepository.save(
              RecruitmentStageEntity.builder()
                  .recruitmentId(recruitment.getId())
                  .stageName("서류 전형")
                  .stageStep(1)
                  .stageType(RecruitmentStageType.DOCUMENT)
                  .build());
      RecruitmentStageEntity interviewStage =
          recruitmentStageRepository.save(
              RecruitmentStageEntity.builder()
                  .recruitmentId(recruitment.getId())
                  .stageName("면접 전형")
                  .stageStep(2)
                  .stageType(RecruitmentStageType.INTERVIEW)
                  .build());

      ApplicationEntity saved =
          applicationRepository.save(
              ApplicationEntity.create(
                  999L,
                  recruitment.getId(),
                  documentStage.getId(),
                  recruitment.getApplicationTemplateId(),
                  "테스트 지원자",
                  Gender.MALE,
                  LocalDateTime.of(1998, 3, 1, 0, 0),
                  "010-1111-2222",
                  "stage-mail@test.com",
                  "{}"));
      applicationId = saved.getId();
      nextStageId = interviewStage.getId();
    } finally {
      TenantContext.clear();
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    headers.set("X-Tenant-ID", tenantId);
    HttpEntity<ApplicationStageUpdateRequest> request =
        new HttpEntity<>(new ApplicationStageUpdateRequest(nextStageId), headers);

    ResponseEntity<ApiResponse> response =
        restTemplate.exchange(
            "/api/v1/applications/" + applicationId + "/stage",
            HttpMethod.PATCH,
            request,
            ApiResponse.class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getResult()).isEqualTo(ResultType.SUCCESS);

    TenantContext.setTenantId(tenantId);
    try {
      assertThat(applicationRepository.findByIdAndStatus(applicationId, EntityStatus.ACTIVE))
          .get()
          .extracting(ApplicationEntity::getRecruitmentProcessId)
          .isEqualTo(nextStageId);

      assertThat(applicationProcessHistoryRepository.findByApplicationId(applicationId))
          .hasSize(1)
          .first()
          .extracting(
              history -> history.getApplicationId(), history -> history.getToRecruitmentProcessId())
          .containsExactly(applicationId, nextStageId);
    } finally {
      TenantContext.clear();
    }
  }
}
