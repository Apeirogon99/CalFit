package com.coffiness.calfit.api.group.list;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/groups")
public class GET_specs {

  @Test
  void 정상_요청_시_200_OK와_그룹_리스트를_반환한다() {}

  @Test
  void 비인가_사용자면_401_Unauthorized를_반환한다() {}

  @Test
  void 그룹_정보의_이름_색상_멤버수가_정확히_반환된다() {}

  @Test
  void 다른_워크스페이스의_그룹은_포함되지_않는다() {}

  @Test
  void 그룹이_없으면_빈_리스트를_반환한다() {}
}
