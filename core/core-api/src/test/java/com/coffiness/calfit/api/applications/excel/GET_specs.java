package com.coffiness.calfit.api.applications.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.ApplicationExcelFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("GET /api/v1/applications/excel")
public class GET_specs {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Test
  void hr_can_download_applicant_excel(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationExcelFixture applicationExcelFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired RecruitmentRepository recruitmentRepository,
      @Autowired RecruitmentStageRepository recruitmentStageRepository,
      @Autowired ApplicationRepository applicationRepository)
      throws Exception {
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    Long hrUserId = userFixture.me(workspace.hrToken()).getData().id();
    Long leadGroupId = findLeadGroupId(workspace.workspaceId(), groupRepository);
    Long templateId =
        createApplicationTemplate(
            workspace.workspaceId(), applicationTemplateRepository, "excel-export-template");
    Long recruitmentId =
        createRecruitment(
            workspace.workspaceId(),
            recruitmentRepository,
            hrUserId,
            templateId,
            leadGroupId,
            "backend-recruitment");
    Long stageId =
        createRecruitmentStage(
            workspace.workspaceId(), recruitmentStageRepository, recruitmentId, "Document Review");

    String applicantEmail = "candidate1@test.com";
    ApiResponse<ApplicantResponse> applicantSignUpResponse =
        applicantFixture.signUp(
            workspace.workspaceId(),
            applicantEmail,
            applicantFixture.randomPassword(),
            "candidate-one");

    InterviewApplicantTestHelper.createPendingApplication(
        workspace.workspaceId(),
        applicationRepository,
        recruitmentId,
        stageId,
        templateId,
        applicantSignUpResponse.getData().id(),
        "candidate-one",
        applicantEmail);

    ResponseEntity<byte[]> response =
        applicationExcelFixture.export(workspace.hrToken(), workspace.workspaceId());

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment;")
        .contains(".xlsx");
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
    assertThat(response.getBody()).isNotNull();

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("지원자 목록");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("지원자명");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("이메일");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue()).isEqualTo("공고");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue())
          .isEqualTo("진행 상태");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(4).getStringCellValue())
          .isEqualTo("다음 일정");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(5).getStringCellValue()).isEqualTo("지원일");
      assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
          .isEqualTo("candidate-one");
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue())
          .isEqualTo(applicantEmail);
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue())
          .isEqualTo("backend-recruitment");
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue())
          .isEqualTo("Document Review");
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue()).isEqualTo("-");
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue())
          .matches("\\d{4}\\.\\d{2}\\.\\d{2}");
    }
  }

  @Test
  void hr_can_download_header_only_excel_even_when_search_result_is_empty(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationExcelFixture applicationExcelFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired RecruitmentRepository recruitmentRepository,
      @Autowired RecruitmentStageRepository recruitmentStageRepository,
      @Autowired ApplicationRepository applicationRepository)
      throws Exception {
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    Long hrUserId = userFixture.me(workspace.hrToken()).getData().id();
    Long leadGroupId = findLeadGroupId(workspace.workspaceId(), groupRepository);
    Long templateId =
        createApplicationTemplate(
            workspace.workspaceId(), applicationTemplateRepository, "excel-export-template");
    Long recruitmentId =
        createRecruitment(
            workspace.workspaceId(),
            recruitmentRepository,
            hrUserId,
            templateId,
            leadGroupId,
            "backend-recruitment");
    Long stageId =
        createRecruitmentStage(
            workspace.workspaceId(), recruitmentStageRepository, recruitmentId, "Document Review");

    String applicantEmail = "candidate2@test.com";
    ApiResponse<ApplicantResponse> applicantSignUpResponse =
        applicantFixture.signUp(
            workspace.workspaceId(),
            applicantEmail,
            applicantFixture.randomPassword(),
            "candidate-two");

    InterviewApplicantTestHelper.createPendingApplication(
        workspace.workspaceId(),
        applicationRepository,
        recruitmentId,
        stageId,
        templateId,
        applicantSignUpResponse.getData().id(),
        "candidate-two",
        applicantEmail);

    ResponseEntity<byte[]> response =
        applicationExcelFixture.export(
            workspace.hrToken(), workspace.workspaceId(), "no-such-applicant");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
    assertThat(response.getBody()).isNotNull();

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("지원자 목록");
      assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
      assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(0);
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("지원자명");
    }
  }

  @Test
  void applicant_cannot_download_applicant_excel(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired ApplicationExcelFixture applicationExcelFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    String applicantEmail = applicantFixture.randomEmail();
    String applicantPassword = applicantFixture.randomPassword();
    applicantFixture.signUp(
        workspace.workspaceId(), applicantEmail, applicantPassword, applicantFixture.randomName());
    ApiResponse<ApplicantLoginResponse> applicantLoginResponse =
        applicantFixture.login(workspace.workspaceId(), applicantEmail, applicantPassword);

    ResponseEntity<byte[]> response =
        applicationExcelFixture.export(
            applicantLoginResponse.getData().accessToken(), workspace.workspaceId());

    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(new String(response.getBody(), StandardCharsets.UTF_8))
        .contains("\"code\":\"E401\"");
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("seed-group", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createApplicationTemplate(
      String tenantId,
      ApplicationTemplateRepository applicationTemplateRepository,
      String templateName) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(
              ApplicationTemplateEntity.create(
                  templateName, "[{\"key\":\"portfolioUrl\",\"type\":\"TEXT\"}]", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createRecruitment(
      String tenantId,
      RecruitmentRepository recruitmentRepository,
      Long creatorId,
      Long templateId,
      Long leadGroupId,
      String title) {
    TenantContext.setTenantId(tenantId);
    try {
      return recruitmentRepository
          .save(
              RecruitmentEntity.builder()
                  .creatorId(creatorId)
                  .title(title)
                  .recruitmentStatus(RecruitmentStatus.OPEN)
                  .targetCount(1)
                  .startDate(LocalDateTime.now().minusDays(1))
                  .endDate(LocalDateTime.now().plusDays(7))
                  .applicationTemplateId(templateId)
                  .contents("recruitment-contents")
                  .careerType(CareerType.NEW)
                  .minExperienceYears(0)
                  .maxExperienceYears(0)
                  .leadGroupId(leadGroupId)
                  .build())
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createRecruitmentStage(
      String tenantId,
      RecruitmentStageRepository recruitmentStageRepository,
      Long recruitmentId,
      String stageName) {
    TenantContext.setTenantId(tenantId);
    try {
      return recruitmentStageRepository
          .save(
              RecruitmentStageEntity.builder()
                  .recruitmentId(recruitmentId)
                  .stageName(stageName)
                  .stageStep(1)
                  .stageType(RecruitmentStageType.DOCUMENT)
                  .build())
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
