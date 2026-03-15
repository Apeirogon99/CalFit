package com.coffiness.calfit.api.subscription.change;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("PATCH /api/v1/subscriptions/current")
public class PATCH_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 결제_수단이_등록되지_않은_상태에서_요청하면_400_Bad_Request를_반환한다() {}

  @Test
  void 존재하지_않는_요금제를_요청하면_400_Bad_Request를_반환한다() {}

  @Test
  void 현재와_동일한_요금제로_변경_시도_시_400_Bad_Request를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void targetPlan이_누락되면_400_Bad_Request를_반환한다() {}

  @Test
  void 변경_후_응답에_newPlan과_changedAt이_포함된다() {}
}
