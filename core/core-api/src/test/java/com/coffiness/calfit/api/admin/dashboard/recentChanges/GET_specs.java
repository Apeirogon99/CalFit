package com.coffiness.calfit.api.admin.dashboard.recentChanges;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GET /api/v1/admin/dashboard/recent-changes
 *
 * <p>구독 변동 이력(신규 가입, 해지, 플랜 변경) 집계 API. 현재 DashboardService에 getRecentChanges() 미구현 상태이므로 전체 비활성화.
 * SubscriptionChangeLog 엔티티 및 이력 트래킹 구현 후 활성화할 것.
 */
@CalfitApiTest
@DisplayName("GET /api/v1/admin/dashboard/recent-changes")
public class GET_specs {

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 변동_내역을_최신순으로_정렬하여_반환한다() {}

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 변동_유형_changeType을_올바르게_반환한다() {}

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 변동_내역이_없으면_빈_리스트를_반환한다() {}

  @Test
  @Disabled("recent-changes 엔드포인트 미구현 — SubscriptionChangeLog 도메인 구현 후 활성화")
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {}
}
