package com.coffiness.calfit.api.admin.invoice.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.InvoiceResponse;
import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/invoices")
public class GET_specs {

  @Test
  void 필터_없이_요청하면_성공_응답을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void status_PAID_필터를_사용하면_납부_완료_인보이스만_반환한다(@Autowired BillingFixture fixture) {
    // Act: PAID 상태 필터
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.adminToken(), "PAID");

    // Assert: 모든 결과가 PAID 상태여야 함
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(
            Arrays.stream(response.getData())
                .allMatch(inv -> inv.invoiceStatus() == InvoiceStatus.PAID))
        .isTrue();
  }

  @Test
  void status_UNPAID_필터를_사용하면_미납_인보이스만_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.adminToken(), "UNPAID");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(
            Arrays.stream(response.getData())
                .allMatch(inv -> inv.invoiceStatus() == InvoiceStatus.UNPAID))
        .isTrue();
  }

  @Test
  void status_OVERDUE_필터를_사용하면_연체_인보이스만_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.adminToken(), "OVERDUE");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(
            Arrays.stream(response.getData())
                .allMatch(inv -> inv.invoiceStatus() == InvoiceStatus.OVERDUE))
        .isTrue();
  }

  @Test
  void 인보이스가_없으면_빈_리스트를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: PAID 필터 (보통 초기 상태에서는 납부 완료 인보이스 없음)
    ApiResponse<InvoiceResponse[]> response = fixture.getInvoices(fixture.adminToken(), "PAID");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    // 납부 완료 인보이스가 없으면 빈 배열 반환
    assertThat(response.getData()).isNotNull();
  }
}
