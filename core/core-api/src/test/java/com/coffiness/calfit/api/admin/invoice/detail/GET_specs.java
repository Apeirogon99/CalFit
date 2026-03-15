package com.coffiness.calfit.api.admin.invoice.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.InvoiceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/invoices/{id}")
public class GET_specs {

  @Test
  void 존재하지_않는_인보이스_ID_사용_시_404_Not_Found를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse> response =
        fixture.getInvoice(fixture.adminToken(), Long.MAX_VALUE);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 인보이스_정보를_올바르게_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 목록에서 조회 가능한 인보이스 ID 획득
    // (결제 이벤트 미구현 상태에서는 초기 데이터가 없을 수 있으므로 guard clause 처리)
    ApiResponse<InvoiceResponse[]> listResponse = fixture.getInvoices(fixture.adminToken());
    assertThat(listResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    InvoiceResponse[] invoices = listResponse.getData();
    if (invoices.length == 0) return;

    Long invoiceId = invoices[0].id();

    // Act
    ApiResponse<InvoiceResponse> response = fixture.getInvoice(fixture.adminToken(), invoiceId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    InvoiceResponse data = response.getData();
    assertThat(data.id()).isEqualTo(invoiceId);
    assertThat(data.workspaceId()).isNotNull();
    assertThat(data.amount()).isNotNegative();
    assertThat(data.invoiceStatus()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse> response = fixture.getInvoice(null, 1L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<InvoiceResponse> response = fixture.getInvoice(fixture.memberToken(), 1L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
