package com.coffiness.calfit.api.admin.dashboard.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.DashboardSummaryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/dashboard/summary")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<DashboardSummaryResponse> response = fixture.getDashboardSummary(null, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자의_접근_토큰을_사용하면_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.memberToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 응답_데이터에_필수_필드가_모두_포함된다(@Autowired BillingFixture fixture) {
    // Act
    DashboardSummaryResponse data =
        fixture.getDashboardSummary(fixture.adminToken(), null).getData();

    // Assert: 모든 수치 필드가 0 이상
    assertThat(data.mrr()).isGreaterThanOrEqualTo(0);
    assertThat(data.mrrGrowth()).isNotNaN();
    assertThat(data.subscribers()).isGreaterThanOrEqualTo(0);
    assertThat(data.newThisMonth()).isGreaterThanOrEqualTo(0);
    assertThat(data.churnRate()).isGreaterThanOrEqualTo(0);
    assertThat(data.totalCost()).isGreaterThanOrEqualTo(0);
    assertThat(data.costGrowth()).isNotNaN();
  }

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다(@Autowired BillingFixture fixture) {
    // Act: month 없이 요청
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 데이터가_없는_월을_조회하면_모든_값이_0으로_반환된다(@Autowired BillingFixture fixture) {
    // Act: 데이터가 없는 먼 과거 월 조회
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.adminToken(), "1800-01");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    DashboardSummaryResponse data = response.getData();
    assertThat(data.mrr()).isZero();
    assertThat(data.subscribers()).isZero();
    assertThat(data.newThisMonth()).isZero();
    assertThat(data.totalCost()).isZero();
  }

  @Test
  void mrrGrowth는_전월_대비_증감률을_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 빈 데이터베이스에서 두 달 연속 0이면 growth = 0
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.adminToken(), "1800-02");

    // Assert: 데이터 없는 두 달 모두 0 → growth = 0.0
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().mrrGrowth()).isEqualTo(0.0);
  }

  @Test
  void 잘못된_month_형식이면_500_오류를_반환한다(@Autowired BillingFixture fixture) {
    // Act: YYYY-MM 형식이 아닌 값 전달 → YearMonth.parse() 예외 발생
    ApiResponse<DashboardSummaryResponse> response =
        fixture.getDashboardSummary(fixture.adminToken(), "not-a-month");

    // Assert: 서버 에러 응답
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
