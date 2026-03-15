package com.coffiness.calfit.api.recruitment.publish;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("PATCH /api/v1/recruitments/{recruitmentId}/publish")
public class PATCH_specs {

  @Test
  void HR은_DRAFT_공고를_즉시_게시할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
    LocalDateTime scheduledStartAt = LocalDateTime.now().plusDays(2);

    Long recruitmentId =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            createDraftRequest(templateId, leadGroupId, hrUserId, scheduledStartAt));

    LocalDateTime beforePublish = LocalDateTime.now().minusSeconds(1);

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.publishRecruitment(hrToken, tenantId, recruitmentId);

    LocalDateTime afterPublish = LocalDateTime.now().plusSeconds(1);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().recruitmentStatus()).isEqualTo(RecruitmentStatus.OPEN);
    assertThat(response.getData().startDate()).isBetween(beforePublish, afterPublish);
    assertThat(response.getData().startDate()).isBefore(scheduledStartAt);
  }

  @Test
  void HR이_아닌_면접관은_수동_게시를_할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, tenantId, hrToken, "수동 게시 권한 없음");

    Long recruitmentId =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            createDraftRequest(templateId, leadGroupId, hrUserId, LocalDateTime.now().plusDays(2)));

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.publishRecruitment(interviewer.token(), tenantId, recruitmentId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이미_게시된_공고는_다시_즉시_게시할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    Long recruitmentId =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            createOpenRequest(templateId, leadGroupId, hrUserId));

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.publishRecruitment(hrToken, tenantId, recruitmentId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  private RecruitmentCreateRequest createDraftRequest(
      Long templateId, Long leadGroupId, Long interviewerUserId, LocalDateTime startDate) {
    return new RecruitmentCreateRequest(
        "즉시 게시 테스트 공고 " + System.nanoTime(),
        2,
        templateId,
        "즉시 게시 확인용 공고",
        startDate,
        startDate.plusDays(7),
        CareerType.NEW,
        null,
        null,
        leadGroupId,
        List.of(),
        List.of(interviewerUserId),
        createStages());
  }

  private RecruitmentCreateRequest createOpenRequest(
      Long templateId, Long leadGroupId, Long interviewerUserId) {
    LocalDateTime now = LocalDateTime.now();
    return new RecruitmentCreateRequest(
        "이미 게시된 공고 " + System.nanoTime(),
        2,
        templateId,
        "이미 게시된 공고",
        now.minusDays(1),
        now.plusDays(7),
        CareerType.NEW,
        null,
        null,
        leadGroupId,
        List.of(),
        List.of(interviewerUserId),
        createStages());
  }

  private Long createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      RecruitmentCreateRequest request) {
    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    return recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
        .filter(item -> item.title().equals(request.title()))
        .findFirst()
        .orElseThrow()
        .id();
  }

  private List<RecruitmentStageRequest> createStages() {
    return List.of(
        new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
        new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
        new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("즉시 게시 테스트 그룹", "#14B8A6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(ApplicationTemplateEntity.create("즉시 게시 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private InterviewerContext inviteInterviewer(
      MemberFixture memberFixture,
      UserFixture userFixture,
      String tenantId,
      String hrToken,
      String name) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    Long userId = userFixture.signUp(email, password, name).getData().id();
    String token = userFixture.login(email, password).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, token);

    return new InterviewerContext(token, userId);
  }

  private record InterviewerContext(String token, Long userId) {}
}
