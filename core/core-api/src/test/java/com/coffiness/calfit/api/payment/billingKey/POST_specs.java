package com.coffiness.calfit.api.payment.billingKey;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.CardInfoResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/payments/billing-key")
class POST_specs {

  final PaymentFixture paymentFixture;

  POST_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void customerKey가_비어있으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.registerBillingKeyRaw(
            ctx.token(), ctx.workspaceId(), Map.of("customerKey", "", "authKey", "test-auth-key"));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void authKey가_비어있으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.registerBillingKeyRaw(
            ctx.token(), ctx.workspaceId(), Map.of("customerKey", "cust_test", "authKey", ""));

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 요청_바디가_없으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.registerBillingKeyRaw(ctx.token(), ctx.workspaceId(), Map.of());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.registerBillingKey(null, ctx.workspaceId(), "cust_test", "auth_test");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 유효하지_않은_authKey로_요청하면_에러를_반환한다() {
    // 토스페이먼츠 테스트 환경에서 잘못된 authKey는 API 에러를 반환
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.registerBillingKey(
            ctx.token(), ctx.workspaceId(), "cust_invalid", "invalid-auth-key");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
