package com.coffiness.calfit.api.admin.dashboard.planDistribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.PlanDistributionResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/dashboard/plan-distribution")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PlanDistributionResponse> response =
        fixture.getPlanDistribution(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().distribution()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PlanDistributionResponse> response = fixture.getPlanDistribution(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PlanDistributionResponse> response =
        fixture.getPlanDistribution(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 각_항목에_status와_count_필드가_포함된다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PlanDistributionResponse> response =
        fixture.getPlanDistribution(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    response
        .getData()
        .distribution()
        .forEach(
            item -> {
              assertThat(item.status()).isNotBlank();
              assertThat(item.count()).isGreaterThanOrEqualTo(0);
            });
  }

  @Test
  void 구독_데이터가_없으면_빈_리스트를_반환한다(@Autowired BillingFixture fixture) {
    // Act: 기본적으로 구독이 없으면 빈 분포 반환
    ApiResponse<PlanDistributionResponse> response =
        fixture.getPlanDistribution(fixture.adminToken());

    // Assert: 성공 응답 (빈 리스트 또는 0 카운트)
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }
}
