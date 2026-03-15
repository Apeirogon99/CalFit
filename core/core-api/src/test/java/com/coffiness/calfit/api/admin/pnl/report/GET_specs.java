package com.coffiness.calfit.api.admin.pnl.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.PnlReportResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/pnl/report")
public class GET_specs {

  @Test
  void 최근_3개월_데이터를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.adminToken(), 3);

    // Assert: columns 크기 = 3
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().columns()).hasSize(3);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(null, 3);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.memberToken(), 3);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 모든_데이터_배열의_길이가_columns_크기와_동일하다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.adminToken(), 3);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    PnlReportResponse data = response.getData();
    int columnCount = data.columns().size();

    assertThat(data.revenueTotal()).hasSize(columnCount);
    assertThat(data.laborCost()).hasSize(columnCount);
    assertThat(data.infraCost()).hasSize(columnCount);
    assertThat(data.marketingCost()).hasSize(columnCount);
    assertThat(data.otherCost()).hasSize(columnCount);
    assertThat(data.costTotal()).hasSize(columnCount);
    assertThat(data.operatingProfit()).hasSize(columnCount);
  }

  @Test
  void costTotal은_각_비용_카테고리의_합계와_일치한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.adminToken(), 3);

    // Assert: 각 월마다 costTotal[i] == labor[i] + infra[i] + marketing[i] + other[i]
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    PnlReportResponse data = response.getData();
    for (int i = 0; i < data.columns().size(); i++) {
      long expectedCostTotal =
          data.laborCost()[i] + data.infraCost()[i] + data.marketingCost()[i] + data.otherCost()[i];
      assertThat(data.costTotal()[i]).isEqualTo(expectedCostTotal);
    }
  }

  @Test
  void operatingProfit은_매출에서_비용을_뺀_값이다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.adminToken(), 3);

    // Assert: 각 월마다 operatingProfit[i] == revenueTotal[i] - costTotal[i]
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    PnlReportResponse data = response.getData();
    for (int i = 0; i < data.columns().size(); i++) {
      long expectedProfit = data.revenueTotal()[i] - data.costTotal()[i];
      assertThat(data.operatingProfit()[i]).isEqualTo(expectedProfit);
    }
  }

  @Test
  void months_파라미터로_조회_기간을_변경할_수_있다(@Autowired BillingFixture fixture) {
    // Act: 6개월 조회
    ApiResponse<PnlReportResponse> response = fixture.getPnlReport(fixture.adminToken(), 6);

    // Assert: columns 크기 = 6
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().columns()).hasSize(6);
  }
}
