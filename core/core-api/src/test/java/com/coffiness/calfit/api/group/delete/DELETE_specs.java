package com.coffiness.calfit.api.group.delete;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("DELETE /api/v1/groups/{groupId}")
public class DELETE_specs {

  @Test
  void 정상_삭제_시_204를_반환하고_그룹이_목록에서_제거된다() {}

  @Test
  void 존재하지_않는_groupId이면_404_Not_Found를_반환한다() {}

  @Test
  void 삭제_권한_없는_요청자는_403_Forbidden을_반환한다() {}

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 그룹_내_멤버가_있는_상태에서_삭제해도_정상_처리된다() {}
}
