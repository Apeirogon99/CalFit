package com.coffiness.calfit.api.automation_rules.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AutomationRuleFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.AutomationRuleUpsertRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.AutomationRuleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/recruitments/{recruitmentId}/automation-rules")
class POST_specs {

  @Test
  void 같은_단계에_서로_다른_payload의_이메일_규칙은_여러_개_생성할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired AutomationRuleFixture automationRuleFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ObjectMapper objectMapper) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);

    ApiResponse<AutomationRuleResponse> firstResponse =
        automationRuleFixture.createRule(
            recruitment.hrToken(),
            recruitment.tenantId(),
            recruitment.recruitmentId(),
            new AutomationRuleUpsertRequest(
                recruitment.firstStageId(),
                AutomationTriggerType.ON_ENTER,
                AutomationActionType.EMAIL,
                payload(objectMapper, "DOCUMENT_PASS")));
    ApiResponse<AutomationRuleResponse> secondResponse =
        automationRuleFixture.createRule(
            recruitment.hrToken(),
            recruitment.tenantId(),
            recruitment.recruitmentId(),
            new AutomationRuleUpsertRequest(
                recruitment.firstStageId(),
                AutomationTriggerType.ON_ENTER,
                AutomationActionType.EMAIL,
                payload(objectMapper, "FINAL_PASS")));

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<AutomationRuleResponse>> rulesResponse =
        automationRuleFixture.getRules(
            recruitment.hrToken(), recruitment.tenantId(), recruitment.recruitmentId());
    assertThat(rulesResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(rulesResponse.getData()).hasSize(2);
  }

  @Test
  void 같은_단계에_완전히_같은_규칙은_중복_생성할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired AutomationRuleFixture automationRuleFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository,
      @Autowired ObjectMapper objectMapper) {
    RecruitmentContext recruitment =
        createRecruitment(
            userFixture,
            workspaceFixture,
            recruitmentFixture,
            groupRepository,
            applicationTemplateRepository);

    AutomationRuleUpsertRequest request =
        new AutomationRuleUpsertRequest(
            recruitment.firstStageId(),
            AutomationTriggerType.ON_ENTER,
            AutomationActionType.EMAIL,
            payload(objectMapper, "DOCUMENT_PASS"));

    ApiResponse<AutomationRuleResponse> firstResponse =
        automationRuleFixture.createRule(
            recruitment.hrToken(), recruitment.tenantId(), recruitment.recruitmentId(), request);
    ApiResponse<AutomationRuleResponse> secondResponse =
        automationRuleFixture.createRule(
            recruitment.hrToken(), recruitment.tenantId(), recruitment.recruitmentId(), request);

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(secondResponse.getError()).isNotNull();
    assertThat(secondResponse.getError().getCode()).isEqualTo("E400");
  }

  private ObjectNode payload(ObjectMapper objectMapper, String templateCode) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("templateCode", templateCode);
    return payload;
  }

  private RecruitmentContext createRecruitment(
      UserFixture userFixture,
      WorkspaceFixture workspaceFixture,
      RecruitmentFixture recruitmentFixture,
      GroupRepository groupRepository,
      ApplicationTemplateRepository applicationTemplateRepository) {
    String hrToken = userFixture.createUserAndGetToken();
    String tenantId = workspaceFixture.createWorkspace(hrToken).getData().workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(
            hrToken,
            tenantId,
            new RecruitmentCreateRequest(
                "automation-rule-recruitment",
                1,
                applicationTemplateId,
                "automation rule contents",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                CareerType.EXPERIENCED,
                1,
                3,
                leadGroupId,
                List.of(),
                List.of(hrUserId),
                List.of(
                    new RecruitmentStageRequest("document", RecruitmentStageType.DOCUMENT, 1),
                    new RecruitmentStageRequest("interview", RecruitmentStageType.INTERVIEW, 2),
                    new RecruitmentStageRequest("pass", RecruitmentStageType.PASS, 3))));
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData().get(0);
    return new RecruitmentContext(
        hrToken, tenantId, recruitment.id(), recruitment.stages().get(0).id());
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    return withTenant(
        tenantId,
        () ->
            groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
                .findFirst()
                .orElseGet(
                    () ->
                        groupRepository.save(
                            GroupEntity.create("automation-rule-group", "#3B82F6")))
                .getId());
  }

  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    return withTenant(
        tenantId,
        () ->
            applicationTemplateRepository
                .save(ApplicationTemplateEntity.create("automation-rule-template", "{}", true))
                .getId());
  }

  private <T> T withTenant(String tenantId, java.util.function.Supplier<T> supplier) {
    TenantContext.setTenantId(tenantId);
    try {
      return supplier.get();
    } finally {
      TenantContext.clear();
    }
  }

  private record RecruitmentContext(
      String hrToken, String tenantId, Long recruitmentId, Long firstStageId) {}
}
