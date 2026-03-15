package com.coffiness.calfit.api.group.create;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/groups")
public class POST_specs {

  @Test
  void 올바른_요청_시_201과_Location_헤더를_반환한다() {}

  @Test
  void group_name_누락_시_400_Bad_Request를_반환한다() {}

  @Test
  void initialMemberIds_중_존재하지_않는_멤버가_있으면_400_Bad_Request를_반환한다() {}

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 같은_워크스페이스_내_동일한_이름의_그룹이_있으면_에러를_반환한다() {}

  @Test
  void group_name이_30자를_초과하면_400_Bad_Request를_반환한다() {}

  @Test
  void initialMemberIds가_다른_워크스페이스_소속이면_400_Bad_Request를_반환한다() {}
}
