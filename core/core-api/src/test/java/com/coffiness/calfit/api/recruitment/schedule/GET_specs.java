package com.coffiness.calfit.api.recruitment.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments/{recruitmentId}/interview-schedules")
public class GET_specs {

  @Test
  void 채용공고의_월별_면접_일정_조회에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    String recruitmentTitle = "2026년 상반기 백엔드 신입 모집";
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        createApplicationTemplate(token, tenantId, applicationTemplateFixture);

    LocalDateTime now = LocalDateTime.now();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            3,
            applicationTemplateId,
            "구합니다. 개발자",
            now.minusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages);

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> recruitmentTitle.equals(item.title()))
            .findFirst()
            .orElseThrow();
    Long recruitmentId = recruitment.id();
    Long meetingRoomId = meetingRoomFixture.create(token, tenantId, "A회의실", 3, 6).getData().id();
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitmentId,
        recruitment.stages().get(1).id(),
        applicationTemplateId,
        201L,
        "지원자#201",
        "applicant201@coffiness.com");

    ApiResponse<InterviewResponse> createInterviewResponse =
        interviewFixture.create(
            token,
            tenantId,
            recruitmentId,
            recruitment.stages().get(1).id(),
            InterviewRound.FIRST,
            List.of(interviewerId),
            List.of(201L),
            meetingRoomId,
            LocalDateTime.of(2026, 3, 18, 13, 0),
            180,
            "실무 면접");
    assertThat(createInterviewResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String yearMonth = "2026-03";

    // Act
    ApiResponse<List<InterviewScheduleResponse>> response =
        recruitmentFixture.getRecruitmentSchedule(token, tenantId, recruitmentId, yearMonth);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).title()).isEqualTo("2026년 상반기 백엔드 신입 모집 · 면접 전형");
    assertThat(response.getData().get(0).applicantName()).isEqualTo("지원자#201");
    assertThat(response.getData().get(0).meetingRoomId()).isEqualTo(meetingRoomId);
    assertThat(response.getData().get(0).location()).isEqualTo("A회의실 (3층)");
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("면접 일정 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 지원서 템플릿을 생성
  private Long createApplicationTemplate(
      String token, String tenantId, ApplicationTemplateFixture applicationTemplateFixture) {
    return applicationTemplateFixture.createUsedTemplateId(token, tenantId, "recruitment-schedule");
  }
}
