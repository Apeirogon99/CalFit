package com.coffiness.calfit.api.recruitment.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
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
@DisplayName("PUT /api/v1/recruitments/{recruitmentId}")
public class PUT_specs {

  @Test
  void 필수_데이터를_입력하면_채용공고_수정에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired WorkspaceFixture workspaceFixture) {

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

    RecruitmentUpdateRequest updateRequest =
        new RecruitmentUpdateRequest(
            "수정된 제목",
            5,
            2L,
            "수정된 내용",
            now.plusDays(2),
            now.plusDays(10),
            CareerType.EXPERIENCED,
            3,
            5,
            2L,
            List.of(3L, 4L),
            List.of(103L, 104L),
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
      @Autowired WorkspaceFixture workspaceFixture) {

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
            now.minusDays(2),
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
    RecruitmentUpdateRequest updateRequest =
        new RecruitmentUpdateRequest(
            "수정된 제목",
            5,
            2L,
            "수정된 내용",
            now.plusDays(2),
            now.plusDays(10),
            CareerType.EXPERIENCED,
            3,
            5,
            2L,
            List.of(3L, 4L),
            List.of(103L, 104L),
            stages);

    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.updateRecruitment(token, tenantId, recruitmentId, updateRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  // TODO : void 면접관이_비어있으면_수정에_실패한다
}
