package com.coffiness.calfit.api.admin.pnl.export;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GET /api/v1/admin/pnl/export
 *
 * <p>손익계산서 Excel 내보내기 API. 현재 AdminPnlController에 /export 엔드포인트가 미구현 상태. Apache POI 등 Excel 라이브러리
 * 연동 및 컨트롤러 구현 후 활성화할 것.
 */
@CalfitApiTest
@DisplayName("GET /api/v1/admin/pnl/export")
public class GET_specs {

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 올바르게_요청하면_성공_응답을_반환한다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 올바르게_요청하면_Content_Type이_Excel_형식이다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 응답_헤더의_파일명에_조회_월이_포함된다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void Excel_파일에_손익_데이터가_올바르게_포함된다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 인증되지_않은_사용자는_에러_응답을_반환한다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void month_파라미터_없이_요청하면_현재_월_기준으로_응답한다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 잘못된_월_형식으로_요청하면_에러_응답을_반환한다() {}

  @Test
  @Disabled("P&L Excel export 엔드포인트 미구현 — Apache POI 연동 후 활성화")
  void 데이터가_없는_월을_조회하면_빈_Excel_파일을_반환한다() {}
}
