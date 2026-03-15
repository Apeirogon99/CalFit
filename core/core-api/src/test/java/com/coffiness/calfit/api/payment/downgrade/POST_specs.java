package com.coffiness.calfit.api.payment.downgrade;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/payments/downgrade")
class POST_specs {

  final PaymentFixture paymentFixture;

  POST_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 올바르게_요청하면_성공을_반환한다() {
    // 워크스페이스 생성 시 Trial 구독이 자동 생성되므로 다운그레이드 가능
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<Void> response = paymentFixture.downgrade(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 다운그레이드_후_구독_상태가_BUSINESS로_변경된다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    paymentFixture.downgrade(ctx.token(), ctx.workspaceId());

    ApiResponse<SubscriptionResponse> subResponse =
        paymentFixture.getSubscription(ctx.token(), ctx.workspaceId());
    assertThat(subResponse.getData()).isNotNull();
    assertThat(subResponse.getData().planType().name()).isEqualTo("BUSINESS");
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<Void> response = paymentFixture.downgrade(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
