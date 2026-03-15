package com.coffiness.calfit.api.subscription.history;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/subscriptions")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 내역은_최신순으로_정렬된다() {}

  @Test
  void priceAmount는_정수형으로_반환된다() {}

  @Test
  void 구독_내역이_없으면_빈_리스트를_반환한다() {}
}
