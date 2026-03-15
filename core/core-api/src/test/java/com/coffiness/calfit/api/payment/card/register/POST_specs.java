package com.coffiness.calfit.api.payment.card.register;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/payment-cards")
public class POST_specs {

  @Test
  void 올바르게_요청하면_201_Created_상태코드를_반환한다() {}

  @Test
  void 카드_정보가_유효하지_않으면_400_Bad_Request를_반환한다() {}

  @Test
  void 비밀번호가_2자리가_아니면_400_Bad_Request를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 법인_카드는_사업자번호_10자리가_필수이다() {}

  @Test
  void 개인_카드는_생년월일_6자리가_필수이다() {}

  @Test
  void 등록_성공_시_기존_카드는_삭제되고_새_카드가_주_결제_수단이_된다() {}

  @Test
  void cardNumber가_16자리가_아니면_400_Bad_Request를_반환한다() {}

  @Test
  void 만료된_expiryDate이면_400_Bad_Request를_반환한다() {}

  @Test
  void cardType이_허용된_값이_아니면_400_Bad_Request를_반환한다() {}
}
