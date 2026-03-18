package com.coffiness.calfit.api.recruitment.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("PUT /api/v1/recruitments/{recruitmentId}")
public class PUT_specs {

  @Test
  void 필수_데이터를_입력하면_채용공고_수정에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(token, tenantId, applicationTemplateFixture);
    Long updatedApplicationTemplateId =
        createApplicationTemplate(token, tenantId, applicationTemplateFixture);
    Long userId = userFixture.me(token).getData().id();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            applicationTemplateId,
            "구합니다. 개발자",
            now.plusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(userId),
            stages);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    RecruitmentUpdateRequest updateRequest =
        new RecruitmentUpdateRequest(
            "수정된 제목",
            5,
            updatedApplicationTemplateId,
            "수정된 내용",
            now.plusDays(2),
            now.plusDays(10),
            CareerType.EXPERIENCED,
            3,
            5,
            leadGroupId,
            List.of(),
            List.of(userId),
            stages);

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitment(token, tenantId, recruitmentId, updateRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 이미_게시가_시작된_공고는_수정할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(token, tenantId, applicationTemplateFixture);
    Long userId = userFixture.me(token).getData().id();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            applicationTemplateId,
            "구합니다. 개발자",
            now.minusDays(2),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(userId),
            stages);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    // Act
    RecruitmentUpdateRequest updateRequest =
        new RecruitmentUpdateRequest(
            "수정된 제목",
            5,
            applicationTemplateId,
            "수정된 내용",
            now.plusDays(2),
            now.plusDays(10),
            CareerType.EXPERIENCED,
            3,
            5,
            leadGroupId,
            List.of(),
            List.of(userId),
            stages);

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitment(token, tenantId, recruitmentId, updateRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 채용단계_변경은_별도_history로_분리된다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired RecruitmentHistoryRepository recruitmentHistoryRepository) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    Long interviewerId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    LocalDateTime now = LocalDateTime.now();

    RecruitmentCreateRequest createRequest =
        new RecruitmentCreateRequest(
            "history stage 분리 확인용 채용",
            2,
            applicationTemplateId,
            "history stage 분리 확인용 내용",
            now.plusDays(5),
            now.plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            List.of(
                new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 면접", RecruitmentStageType.INTERVIEW, 3),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(hrToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData().stream()
            .filter(item -> "history stage 분리 확인용 채용".equals(item.title()))
            .findFirst()
            .orElseThrow()
            .id();

    RecruitmentUpdateRequest updateRequest =
        new RecruitmentUpdateRequest(
            "history stage 분리 확인용 채용",
            2,
            applicationTemplateId,
            "history stage 분리 확인용 내용",
            now.plusDays(5),
            now.plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            List.of(
                new RecruitmentStageRequest("과제 테스트", RecruitmentStageType.TEST, 1),
                new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 2),
                new RecruitmentStageRequest("최종 기술 면접", RecruitmentStageType.INTERVIEW, 3),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4)));

    ApiResponse<RecruitmentDetailResponse> updateResponse =
        recruitmentFixture.updateRecruitment(hrToken, tenantId, recruitmentId, updateRequest);
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    List<RecruitmentHistoryEntity> histories =
        readRecruitmentHistories(tenantId, recruitmentHistoryRepository, recruitmentId);

    assertThat(histories)
        .extracting(RecruitmentHistoryEntity::getRecruitmentActionType)
        .contains(RecruitmentActionType.RECRUITMENT_CREATED)
        .contains(RecruitmentActionType.STAGE_CREATED)
        .contains(RecruitmentActionType.STAGE_UPDATED)
        .contains(RecruitmentActionType.STAGE_DELETED)
        .contains(RecruitmentActionType.STAGE_REORDERED)
        .doesNotContain(RecruitmentActionType.RECRUITMENT_INFO_UPDATED);

    assertThat(histories)
        .filteredOn(
            history -> history.getRecruitmentActionType() == RecruitmentActionType.STAGE_REORDERED)
        .singleElement()
        .satisfies(
            history -> {
              assertThat(history.getStageId()).isNotNull();
              @SuppressWarnings("unchecked")
              Map<String, Object> changeLog = (Map<String, Object>) history.getChangeLog();
              assertThat(changeLog)
                  .containsEntry("scope", "STAGE")
                  .containsKeys("before", "after", "changedFields");
              assertThat((List<String>) changeLog.get("changedFields"))
                  .containsExactly("stageStep");
            });
  }

  // TODO : void 면접관이_비어있으면_수정에_실패한다

  private List<RecruitmentHistoryEntity> readRecruitmentHistories(
      String tenantId,
      RecruitmentHistoryRepository recruitmentHistoryRepository,
      Long recruitmentId) {
    TenantContext.setTenantId(tenantId);
    try {
      return recruitmentHistoryRepository.findAll().stream()
          .filter(history -> recruitmentId.equals(history.getRecruitmentId()))
          .toList();
    } finally {
      TenantContext.clear();
    }
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("수정 테스트 그룹", "#14B8A6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private Long createApplicationTemplate(
      String token, String tenantId, ApplicationTemplateFixture applicationTemplateFixture) {
    return applicationTemplateFixture.createUsedTemplateId(token, tenantId, "recruitment-update");
  }
}
