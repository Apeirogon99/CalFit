package com.coffiness.calfit.api.payment.billingEmail.info;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/billing-emails")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 등록된_이메일이_없을_경우_email_필드는_null을_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}
}
