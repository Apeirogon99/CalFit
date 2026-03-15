package com.coffiness.calfit.api.payment.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.fixture.PaymentFixture.PaymentContext;
import com.coffiness.calfit.api.v1.response.CardInfoResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/payments/card")
class GET_specs {

  final PaymentFixture paymentFixture;

  GET_specs(PaymentFixture paymentFixture) {
    this.paymentFixture = paymentFixture;
  }

  @Test
  void 등록된_카드가_없으면_null을_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response = paymentFixture.getCard(ctx.token(), ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_에러를_반환한다() {
    PaymentContext ctx = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response = paymentFixture.getCard(null, ctx.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
