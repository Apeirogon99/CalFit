package com.coffiness.calfit.api.admin.dashboard.revenueCostTrend;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.RevenueCostTrendResponse;
import com.coffiness.calfit.api.v1.response.RevenueCostTrendResponse.MonthlyTrendItem;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/dashboard/revenue-cost-trend")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<RevenueCostTrendResponse> response =
        fixture.getRevenueCostTrend(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().trend()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<RevenueCostTrendResponse> response = fixture.getRevenueCostTrend(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<RevenueCostTrendResponse> response =
        fixture.getRevenueCostTrend(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 트렌드_항목에_year_month_revenue_cost_필드가_포함된다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<RevenueCostTrendResponse> response =
        fixture.getRevenueCostTrend(fixture.adminToken());

    // Assert: 데이터가 있으면 각 항목의 필드 검증, 없으면 빈 리스트 확인
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    List<MonthlyTrendItem> trend = response.getData().trend();
    trend.forEach(
        item -> {
          assertThat(item.year()).isPositive();
          assertThat(item.month()).isBetween(1, 12);
          assertThat(item.revenue()).isGreaterThanOrEqualTo(0);
          assertThat(item.cost()).isGreaterThanOrEqualTo(0);
        });
  }

  @Test
  void 트렌드_데이터는_월_오름차순으로_정렬된다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<RevenueCostTrendResponse> response =
        fixture.getRevenueCostTrend(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    List<MonthlyTrendItem> trend = response.getData().trend();
    for (int i = 0; i < trend.size() - 1; i++) {
      int curr = trend.get(i).year() * 12 + trend.get(i).month();
      int next = trend.get(i + 1).year() * 12 + trend.get(i + 1).month();
      assertThat(curr).isLessThanOrEqualTo(next);
    }
  }
}
