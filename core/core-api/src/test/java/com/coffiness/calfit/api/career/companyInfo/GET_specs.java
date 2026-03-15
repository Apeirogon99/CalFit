package com.coffiness.calfit.api.career.companyInfo;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/careers/{companySlug}")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 존재하지_않는_companySlug로_요청하면_404_Not_Found를_반환한다() {}

  @Test
  void 회사_정보의_slug_name_introduction이_올바르게_반환된다() {}
}
