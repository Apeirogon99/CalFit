package com.coffiness.calfit.api.group.addMember;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/groups/{groupId}/members")
public class POST_specs {

  @Test
  void 정상_요청_시_204를_반환하고_그룹의_memberCount가_증가한다() {}

  @Test
  void 존재하지_않는_memberId이면_404_Not_Found를_반환한다() {}

  @Test
  void 권한_없는_사용자는_403_Forbidden을_반환한다() {}

  @Test
  void 이미_그룹에_속한_멤버를_추가하면_에러를_반환한다() {}

  @Test
  void 다른_워크스페이스_소속_멤버를_추가하면_에러를_반환한다() {}

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 존재하지_않는_groupId이면_404_Not_Found를_반환한다() {}
}
