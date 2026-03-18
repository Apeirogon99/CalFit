package com.coffiness.calfit.api.recruitment.interviewers;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
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
@DisplayName("PATCH /api/v1/recruitments/{recruitmentId}/interviewers")
public class PATCH_specs {

  @Test
  void 이미_게시된_공고도_면접관은_수정할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, tenantId, hrToken, "추가 면접관");
    Long recruitmentId =
        createRecruitment(
            recruitmentFixture, hrToken, tenantId, applicationTemplateId, leadGroupId, hrUserId);

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitmentInterviewers(
            hrToken,
            tenantId,
            recruitmentId,
            Map.of("interviewerIds", List.of(hrUserId, interviewer.userId())));

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewers())
        .extracting(item -> item.userId())
        .containsExactlyInAnyOrder(hrUserId, interviewer.userId());
  }

  @Test
  void HR이_아닌_면접관은_면접관_설정을_수정할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, tenantId, hrToken, "권한 없는 면접관");
    Long recruitmentId =
        createRecruitment(
            recruitmentFixture, hrToken, tenantId, applicationTemplateId, leadGroupId, hrUserId);

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitmentInterviewers(
            interviewer.token(),
            tenantId,
            recruitmentId,
            Map.of("interviewerIds", List.of(interviewer.userId())));

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접관은_최소_한_명_이상이어야_한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    Long recruitmentId =
        createRecruitment(
            recruitmentFixture, hrToken, tenantId, applicationTemplateId, leadGroupId, hrUserId);

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitmentInterviewers(
            hrToken, tenantId, recruitmentId, Map.of("interviewerIds", List.of()));

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접관_추가_및_해제시_history_changeLog에_대상_id를_남긴다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired RecruitmentHistoryRepository recruitmentHistoryRepository) {

    WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(hrToken, tenantId, applicationTemplateFixture);
    Long recruitmentId =
        createRecruitment(
            recruitmentFixture, hrToken, tenantId, applicationTemplateId, leadGroupId, hrUserId);

    InterviewerContext newInterviewer =
        inviteInterviewer(memberFixture, userFixture, tenantId, hrToken, "교체 면접관");

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitmentInterviewers(
            hrToken,
            tenantId,
            recruitmentId,
            Map.of("interviewerIds", List.of(newInterviewer.userId())));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    List<RecruitmentHistoryEntity> histories =
        readRecruitmentHistories(tenantId, recruitmentHistoryRepository, recruitmentId);

    assertThat(histories)
        .extracting(RecruitmentHistoryEntity::getRecruitmentActionType)
        .contains(RecruitmentActionType.INTERVIEWER_ADDED)
        .contains(RecruitmentActionType.INTERVIEWER_REMOVED);

    assertThat(histories)
        .filteredOn(
            history ->
                history.getRecruitmentActionType() == RecruitmentActionType.INTERVIEWER_ADDED)
        .singleElement()
        .satisfies(
            history -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> changeLog = (Map<String, Object>) history.getChangeLog();
              assertThat(changeLog)
                  .containsEntry("scope", "INTERVIEWER")
                  .containsEntry("changedFields", List.of("interviewerIds"));
              assertThat(extractInterviewerIds(changeLog, "before")).containsExactly(hrUserId);
              assertThat(extractInterviewerIds(changeLog, "after"))
                  .containsExactly(hrUserId, newInterviewer.userId());
            });

    assertThat(histories)
        .filteredOn(
            history ->
                history.getRecruitmentActionType() == RecruitmentActionType.INTERVIEWER_REMOVED)
        .singleElement()
        .satisfies(
            history -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> changeLog = (Map<String, Object>) history.getChangeLog();
              assertThat(changeLog)
                  .containsEntry("scope", "INTERVIEWER")
                  .containsEntry("changedFields", List.of("interviewerIds"));
              assertThat(extractInterviewerIds(changeLog, "before"))
                  .containsExactly(hrUserId, newInterviewer.userId());
              assertThat(extractInterviewerIds(changeLog, "after"))
                  .containsExactly(newInterviewer.userId());
            });
  }

  // 테스트용 게시된 공고를 하나 생성
  private Long createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      Long applicationTemplateId,
      Long leadGroupId,
      Long interviewerUserId) {
    LocalDateTime now = LocalDateTime.now();
    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "면접관 설정 테스트 공고",
            2,
            applicationTemplateId,
            "게시 후 면접관 변경 가능 여부 확인",
            now.minusDays(1),
            now.plusDays(10),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerUserId),
            List.of(
                new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    return recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
        .filter(item -> item.title().equals("면접관 설정 테스트 공고"))
        .findFirst()
        .orElseThrow()
        .id();
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("면접관 수정 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 지원서 템플릿을 생성
  private Long createApplicationTemplate(
      String token, String tenantId, ApplicationTemplateFixture applicationTemplateFixture) {
    return applicationTemplateFixture.createUsedTemplateId(
        token, tenantId, "recruitment-interviewers");
  }

  // 면접관 멤버를 초대하고 토큰과 userId를 함께 반환
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

  private List<RecruitmentHistoryEntity> readRecruitmentHistories(
      String tenantId,
      RecruitmentHistoryRepository recruitmentHistoryRepository,
      Long recruitmentId) {
    TenantContext.setTenantId(tenantId);
    try {
      return recruitmentHistoryRepository.findByRecruitmentIdOrderByCreatedAtAsc(recruitmentId);
    } finally {
      TenantContext.clear();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Long> extractInterviewerIds(Map<String, Object> changeLog, String key) {
    Map<String, Object> snapshot = (Map<String, Object>) changeLog.get(key);
    return ((List<?>) snapshot.get("interviewerIds"))
        .stream().map(Number.class::cast).map(Number::longValue).toList();
  }

  private record InterviewerContext(String token, Long userId) {}
}
