package com.coffiness.calfit.api.career.jobs;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/careers/{companySlug}/jobs")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 검색_결과가_없을_경우_빈_리스트를_반환한다() {}

  @Test
  void 카테고리가_유효하지_않은_값이면_400_Bad_Request를_반환한다() {}

  @Test
  void search_파라미터로_제목_검색이_올바르게_동작한다() {}

  @Test
  void category_파라미터로_직군별_필터링이_동작한다() {}

  @Test
  void 검색_조건이_없으면_전체_목록을_반환한다() {}

  @Test
  void search와_category를_동시에_사용해도_올바르게_동작한다() {}

  @Test
  void 존재하지_않는_companySlug로_요청하면_404_Not_Found를_반환한다() {}
}
