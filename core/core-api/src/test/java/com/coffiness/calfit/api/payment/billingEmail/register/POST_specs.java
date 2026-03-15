package com.coffiness.calfit.api.payment.billingEmail.register;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/billing-emails")
public class POST_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 이메일_형식이_잘못되면_400_Bad_Request를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void email_필드가_누락되면_400_Bad_Request를_반환한다() {}
}
