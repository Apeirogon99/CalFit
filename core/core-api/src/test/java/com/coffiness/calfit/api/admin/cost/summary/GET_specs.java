package com.coffiness.calfit.api.admin.cost.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.CostSummaryResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/costs/summary")
public class GET_specs {

  private static final int TEST_YEAR = 1992;
  private static final int TEST_MONTH = 4;

  @Test
  void 카테고리를_모두_반환한다(@Autowired BillingFixture fixture) {
    // Act: 빈 월도 4개 카테고리 모두 0으로 반환되어야 한다
    ApiResponse<CostSummaryResponse> response =
        fixture.getCostSummary(fixture.adminToken(), "2000-01");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().byCategory())
        .containsKey(CostCategory.LABOR)
        .containsKey(CostCategory.INFRASTRUCTURE)
        .containsKey(CostCategory.MARKETING)
        .containsKey(CostCategory.OTHER);
  }

  @Test
  void total이_카테고리별_금액의_합계와_일치한다(@Autowired BillingFixture fixture) {
    // Arrange: 각 카테고리에 비용 생성
    String token = fixture.adminToken();
    String month = TEST_YEAR + "-" + String.format("%02d", TEST_MONTH);
    fixture.createCost(token, CostCategory.LABOR, "인건비", 3_000_000L, TEST_YEAR, TEST_MONTH);
    fixture.createCost(token, CostCategory.INFRASTRUCTURE, "서버", 500_000L, TEST_YEAR, TEST_MONTH);
    fixture.createCost(token, CostCategory.MARKETING, "광고", 200_000L, TEST_YEAR, TEST_MONTH);
    fixture.createCost(token, CostCategory.OTHER, "기타", 100_000L, TEST_YEAR, TEST_MONTH);

    // Act
    ApiResponse<CostSummaryResponse> response = fixture.getCostSummary(token, month);

    // Assert: total == sum of all category amounts
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    long expectedTotal =
        response.getData().byCategory().values().stream().mapToLong(Long::longValue).sum();
    assertThat(response.getData().total()).isEqualTo(expectedTotal);
  }

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다(@Autowired BillingFixture fixture) {
    // Act: month 없이 요청
    ApiResponse<CostSummaryResponse> response = fixture.getCostSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 데이터가_없는_월에는_모든_카테고리_금액이_0이다(@Autowired BillingFixture fixture) {
    // Act: 데이터가 없는 먼 과거 월 조회
    ApiResponse<CostSummaryResponse> response =
        fixture.getCostSummary(fixture.adminToken(), "1800-01");

    // Assert: 모든 카테고리 = 0, total = 0
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().total()).isZero();
    response.getData().byCategory().values().forEach(v -> assertThat(v).isZero());
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostSummaryResponse> response = fixture.getCostSummary(null, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostSummaryResponse> response = fixture.getCostSummary(fixture.memberToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
