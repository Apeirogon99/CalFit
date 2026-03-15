package com.coffiness.calfit.api.subscription.current;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/subscriptions/current")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 구독_중인_요금제가_없으면_FREE_상태를_반환한다() {}

  @Test
  void 등록된_결제_카드가_없으면_paymentMethod는_null을_반환한다() {}
}
