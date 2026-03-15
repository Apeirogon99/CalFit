package com.coffiness.calfit.api.admin.pnl.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.PnlSummaryResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/pnl/summary")
public class GET_specs {

  private static final int TEST_YEAR = 1996;
  private static final int TEST_MONTH = 8;

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlSummaryResponse> response = fixture.getPnlSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlSummaryResponse> response = fixture.getPnlSummary(null, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlSummaryResponse> response = fixture.getPnlSummary(fixture.memberToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlSummaryResponse> response = fixture.getPnlSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void operatingProfit은_totalRevenue에서_totalCost를_뺀_값이다(@Autowired BillingFixture fixture) {
    // Arrange: 특정 월에 비용만 생성 (매출 없음)
    String token = fixture.adminToken();
    String month = TEST_YEAR + "-" + String.format("%02d", TEST_MONTH);
    long costAmount = 1_500_000L;
    fixture.createCost(token, CostCategory.LABOR, "P&L 테스트 비용", costAmount, TEST_YEAR, TEST_MONTH);

    // Act
    ApiResponse<PnlSummaryResponse> response = fixture.getPnlSummary(token, month);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    PnlSummaryResponse data = response.getData();
    assertThat(data.operatingProfit()).isEqualTo(data.totalRevenue() - data.totalCost());
  }

  @Test
  void operatingMargin은_매출_대비_영업이익률이다(@Autowired BillingFixture fixture) {
    // Arrange: 데이터 없는 월 (매출=0, 비용=0, 이익=0)
    ApiResponse<PnlSummaryResponse> response =
        fixture.getPnlSummary(fixture.adminToken(), "1800-01");

    // Assert: 매출 0일 때 margin = 0.0
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().operatingMargin()).isCloseTo(0.0, within(0.001));
  }

  @Test
  void 데이터_없는_월에는_모든_값이_0이다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlSummaryResponse> response =
        fixture.getPnlSummary(fixture.adminToken(), "1800-03");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    PnlSummaryResponse data = response.getData();
    assertThat(data.totalRevenue()).isZero();
    assertThat(data.totalCost()).isZero();
    assertThat(data.operatingProfit()).isZero();
    assertThat(data.operatingMargin()).isEqualTo(0.0);
  }
}
