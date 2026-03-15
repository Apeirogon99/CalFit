package com.coffiness.calfit.api.payment.subscription;

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
@DisplayName("GET /api/v1/payments/subscription")
class GET_specs {

  final PaymentFixture paymentFixture;

  GET_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 워크스페이스_생성_후_Trial_구독이_자동_생성된다() {
    // WorkspaceCreatedEvent → BillingEventHandler → Trial 구독 생성
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<SubscriptionResponse> response =
        paymentFixture.getSubscription(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().subscriptionStatus().name()).isEqualTo("TRIAL");
    assertThat(response.getData().workspaceId()).isEqualTo(ctx.workspaceId());
  }

  @Test
  void 구독_정보에_요금제와_월_금액이_포함된다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<SubscriptionResponse> response =
        paymentFixture.getSubscription(ctx.token(), ctx.workspaceId());

    assertThat(response.getData().planType()).isNotNull();
    assertThat(response.getData().monthlyAmount()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<SubscriptionResponse> response =
        paymentFixture.getSubscription(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
