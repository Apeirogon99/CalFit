package com.coffiness.calfit.api.admin.subscription.export;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GET /api/v1/admin/subscriptions/export
 *
 * <p>구독 목록 CSV 내보내기 API. 현재 AdminSubscriptionController에 /export 엔드포인트가 미구현 상태. CSV 직렬화 로직 및 컨트롤러
 * 구현 후 활성화할 것.
 */
@CalfitApiTest
@DisplayName("GET /api/v1/admin/subscriptions/export")
public class GET_specs {

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void Content_Type이_text_csv이다() {}

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void CSV_파일에_구독_데이터가_올바르게_포함된다() {}

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void status_필터가_내보내기_결과에도_적용된다() {}

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {}

  @Test
  @Disabled("구독 CSV export 엔드포인트 미구현 — 컨트롤러 구현 후 활성화")
  void 구독_데이터가_없으면_빈_CSV를_반환한다() {}
}
