package com.coffiness.calfit.api.payment.upgrade;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.PaymentResultResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/payments/upgrade")
class POST_specs {

  final PaymentFixture paymentFixture;

  POST_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 빌링키가_등록되지_않은_상태에서_업그레이드하면_에러를_반환한다() {
    // 카드(빌링키) 등록 없이 업그레이드 시도
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentResultResponse> response =
        paymentFixture.upgrade(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentResultResponse> response = paymentFixture.upgrade(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void X_Tenant_ID_헤더가_없으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<PaymentResultResponse> response = paymentFixture.upgrade(ctx.token(), null);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
