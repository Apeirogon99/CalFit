package com.coffiness.calfit.api.payment.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.PaymentHistoryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/payments/history")
class GET_specs {

  final PaymentFixture paymentFixture;

  GET_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 결제_내역이_없으면_빈_배열을_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentHistoryResponse[]> response =
        paymentFixture.getHistory(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }

  @Test
  void 페이지_파라미터가_정상_동작한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentHistoryResponse[]> response =
        paymentFixture.getHistory(ctx.token(), ctx.workspaceId(), 0, 5);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentHistoryResponse[]> response =
        paymentFixture.getHistory(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
