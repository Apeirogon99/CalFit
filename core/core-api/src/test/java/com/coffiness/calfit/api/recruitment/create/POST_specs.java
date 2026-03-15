package com.coffiness.calfit.api.recruitment.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
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
@DisplayName("POST /api/v1/recruitments")
public class POST_specs {

  @Test
  void 필수_데이터를_입력하면_채용공고_생성에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
    RecruitmentCreateRequest request =
        createRequest(applicationTemplateId, leadGroupId, List.of(hrUserId));

    // Act
    ApiResponse<Void> response = recruitmentFixture.createRecruitment(token, tenantId, request);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void HR_권한이_없는_면접관은_채용공고를_생성할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);
    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, tenantId, hrToken);
    RecruitmentCreateRequest request =
        createRequest(applicationTemplateId, leadGroupId, List.of(hrUserId));

    // Act
    ApiResponse<Void> response =
        recruitmentFixture.createRecruitment(interviewer.token(), tenantId, request);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
    assertThat(recruitmentFixture.getRecruitmentList(hrToken, tenantId).getData())
        .extracting(com.coffiness.calfit.api.v1.response.RecruitmentListResponse::title)
        .doesNotContain(request.title());
  }

  // 채용 공고 생성 요청을 테스트용 기본값으로 구성
  private RecruitmentCreateRequest createRequest(
      Long applicationTemplateId, Long leadGroupId, List<Long> interviewerIds) {
    LocalDateTime now = LocalDateTime.now();
    return new RecruitmentCreateRequest(
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
        interviewerIds,
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3)));
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("생성 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 지원서 템플릿을 생성
  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(ApplicationTemplateEntity.create("생성 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 면접관 멤버를 초대하고 로그인 가능한 토큰을 반환
  private InterviewerContext inviteInterviewer(
      MemberFixture memberFixture, UserFixture userFixture, String tenantId, String hrToken) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    userFixture.signUp(email, password, "면접관 사용자");

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken);

    String token = userFixture.login(email, password).getData().accessToken();
    return new InterviewerContext(token);
  }

  private record InterviewerContext(String token) {}
}
