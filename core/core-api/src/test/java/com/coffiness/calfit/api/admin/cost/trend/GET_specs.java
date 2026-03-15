package com.coffiness.calfit.api.admin.cost.trend;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.CostTrendResponse;
import com.coffiness.calfit.api.v1.response.CostTrendResponse.MonthlyCostItem;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/costs/trend")
public class GET_specs {

  @Test
  void 성공_응답과_트렌드_목록을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostTrendResponse> response = fixture.getCostTrend(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().trend()).isNotNull();
  }

  @Test
  void 비용_데이터가_있으면_해당_월이_트렌드에_포함된다(@Autowired BillingFixture fixture) {
    // Arrange: 고유한 연도/월에 비용 생성
    String token = fixture.adminToken();
    fixture.createCost(token, CostCategory.LABOR, "트렌드 테스트 비용", 1_000_000L, 1993, 6);

    // Act
    ApiResponse<CostTrendResponse> response = fixture.getCostTrend(token);

    // Assert: 생성한 월 데이터가 트렌드에 존재
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    List<MonthlyCostItem> trend = response.getData().trend();
    boolean containsTestMonth =
        trend.stream().anyMatch(item -> item.year() == 1993 && item.month() == 6);
    assertThat(containsTestMonth).isTrue();
  }

  @Test
  void 트렌드_데이터는_연도_및_월_기준으로_오름차순_정렬된다(@Autowired BillingFixture fixture) {
    // Arrange: 서로 다른 연도/월에 비용 생성
    String token = fixture.adminToken();
    fixture.createCost(token, CostCategory.OTHER, "이후 비용", 100_000L, 1993, 12);
    fixture.createCost(token, CostCategory.OTHER, "이전 비용", 200_000L, 1993, 7);

    // Act
    ApiResponse<CostTrendResponse> response = fixture.getCostTrend(token);

    // Assert: 정렬 확인 — 이전 연도/월 항목이 더 앞에 위치해야 함
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    List<MonthlyCostItem> trend = response.getData().trend();
    for (int i = 0; i < trend.size() - 1; i++) {
      MonthlyCostItem curr = trend.get(i);
      MonthlyCostItem next = trend.get(i + 1);
      int currOrdinal = curr.year() * 12 + curr.month();
      int nextOrdinal = next.year() * 12 + next.month();
      assertThat(currOrdinal).isLessThanOrEqualTo(nextOrdinal);
    }
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostTrendResponse> response = fixture.getCostTrend(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostTrendResponse> response = fixture.getCostTrend(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
