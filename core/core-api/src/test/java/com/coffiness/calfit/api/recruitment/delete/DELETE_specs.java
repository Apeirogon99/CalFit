package com.coffiness.calfit.api.recruitment.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("DELETE /api/v1/recruitments/{recruitmentId}")
public class DELETE_specs {

  @Test
  void HR_담당자는_채용_공고를_성공적으로_삭제할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            1L,
            "구합니다. 개발자",
            now.plusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(101L, 102L),
            stages);

    recruitmentFixture.createRecruitment(token, tenantId, request);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    // Act
    ApiResponse<Void> response =
        recruitmentFixture.deleteRecruitment(token, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void HR_권한이_없는_일반_멤버나_면접관은_공고를_삭제할_수_없다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired com.coffiness.calfit.api.fixture.MemberFixture memberFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            1L,
            "구합니다. 개발자",
            now.plusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(101L, 102L),
            stages);

    recruitmentFixture.createRecruitment(token, tenantId, request);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    String inviteeEmail = "interviewer@test.com";
    String inviteePassword = "password123!";
    userFixture.signUp(inviteeEmail, inviteePassword, "인터뷰어");

    String invitationToken =
        memberFixture
            .createInvitation(
                token,
                tenantId,
                inviteeEmail,
                com.coffiness.calfit.core.enums.MemberType.INTERVIEWER)
            .getData()
            .token();

    memberFixture.acceptInvitation(invitationToken);

    String unauthorizedToken =
        userFixture.login(inviteeEmail, inviteePassword).getData().accessToken();

    // Act
    ApiResponse<Void> response =
        recruitmentFixture.deleteRecruitment(unauthorizedToken, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 이미_삭제된_공고를_삭제하려고_하면_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            1L,
            "구합니다. 개발자",
            now.plusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(101L, 102L),
            stages);

    recruitmentFixture.createRecruitment(token, tenantId, request);
    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    recruitmentFixture.deleteRecruitment(token, tenantId, recruitmentId);

    // Act
    ApiResponse<Void> response =
        recruitmentFixture.deleteRecruitment(token, tenantId, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  // TODO : 진행중인 공고 삭제 불가능
}
