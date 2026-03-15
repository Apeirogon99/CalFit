package com.coffiness.calfit.api.payment.tossConfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.TossConfigResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/payments/toss-config")
class GET_specs {

  final PaymentFixture paymentFixture;

  GET_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 올바르게_요청하면_clientKey와_customerKey를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<TossConfigResponse> response =
        paymentFixture.getTossConfig(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().clientKey()).isNotBlank();
    assertThat(response.getData().customerKey()).startsWith("cust_");
  }

  @Test
  void customerKey는_워크스페이스ID를_포함한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<TossConfigResponse> response =
        paymentFixture.getTossConfig(ctx.token(), ctx.workspaceId());

    assertThat(response.getData().customerKey()).isEqualTo("cust_" + ctx.workspaceId());
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_응답을_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<TossConfigResponse> response =
        paymentFixture.getTossConfig(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
