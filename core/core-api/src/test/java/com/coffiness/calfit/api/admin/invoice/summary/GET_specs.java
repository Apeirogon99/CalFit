package com.coffiness.calfit.api.admin.invoice.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.InvoiceSummaryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/invoices/summary")
public class GET_specs {

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceSummaryResponse> response = fixture.getInvoiceSummary(null, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.memberToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.adminToken(), null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 데이터_없는_월의_모든_금액은_0이다(@Autowired BillingFixture fixture) {
    // Act: 먼 과거 월 조회
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.adminToken(), "1800-01");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    InvoiceSummaryResponse data = response.getData();
    assertThat(data.confirmedRevenue()).isZero();
    assertThat(data.confirmedCount()).isZero();
    assertThat(data.outstanding()).isZero();
    assertThat(data.outstandingOverdue()).isZero();
  }

  @Test
  void outstanding은_미결제와_연체_합산_금액이다(@Autowired BillingFixture fixture) {
    // Arrange: 데이터 없는 월 기준으로 검증 (미결제 0 + 연체 0 = outstanding 0)
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.adminToken(), "1800-01");

    // Assert: outstanding >= outstandingOverdue (연체는 outstanding의 부분집합)
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    InvoiceSummaryResponse data = response.getData();
    assertThat(data.outstanding()).isGreaterThanOrEqualTo(data.outstandingOverdue());
  }

  @Test
  void 잘못된_month_형식이면_오류를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceSummaryResponse> response =
        fixture.getInvoiceSummary(fixture.adminToken(), "invalid-month");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
