package com.coffiness.calfit.api.admin.cost.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.CostResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/admin/costs")
public class POST_specs {

  private static final int TEST_YEAR = 1990;
  private static final int TEST_MONTH = 1;

  @Test
  void 올바르게_요청하면_생성된_비용_정보를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange
    String token = fixture.adminToken();

    // Act
    ApiResponse<CostResponse> response =
        fixture.createCost(
            token, CostCategory.LABOR, "개발자 급여", 4_500_000L, TEST_YEAR, TEST_MONTH, "월 고정비");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
    assertThat(response.getData().category()).isEqualTo(CostCategory.LABOR);
    assertThat(response.getData().name()).isEqualTo("개발자 급여");
    assertThat(response.getData().amount()).isEqualTo(4_500_000L);
    assertThat(response.getData().costYear()).isEqualTo(TEST_YEAR);
    assertThat(response.getData().costMonth()).isEqualTo(TEST_MONTH);
    assertThat(response.getData().memo()).isEqualTo("월 고정비");
  }

  @Test
  void 필수_속성_누락_시_400_Bad_Request를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: category가 null인 요청
    String token = fixture.adminToken();
    ApiResponse<CostResponse> response =
        fixture.createCost(token, null, "급여", 1_000_000L, TEST_YEAR, TEST_MONTH);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void category가_허용된_값이_아니면_400_Bad_Request를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 허용되지 않는 category 문자열을 Map으로 직접 전송
    String token = fixture.adminToken();
    Map<String, Object> rawRequest = new HashMap<>();
    rawRequest.put("category", "INVALID_CATEGORY");
    rawRequest.put("name", "테스트 비용");
    rawRequest.put("amount", 100_000);
    rawRequest.put("year", TEST_YEAR);
    rawRequest.put("month", TEST_MONTH);

    // Act
    ApiResponse<CostResponse> response = fixture.createCostRaw(token, rawRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void amount가_0_이하이면_400_Bad_Request를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: @Min(1) 위반 — amount = 0
    String token = fixture.adminToken();
    ApiResponse<CostResponse> response =
        fixture.createCost(token, CostCategory.INFRASTRUCTURE, "서버 비용", 0L, TEST_YEAR, TEST_MONTH);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void month가_null이면_400_Bad_Request를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: @NotNull 위반 — month = null
    String token = fixture.adminToken();
    Map<String, Object> rawRequest = new HashMap<>();
    rawRequest.put("category", "MARKETING");
    rawRequest.put("name", "광고비");
    rawRequest.put("amount", 500_000);
    rawRequest.put("year", TEST_YEAR);
    // month 누락

    // Act
    ApiResponse<CostResponse> response = fixture.createCostRaw(token, rawRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act: 토큰 없이 요청
    ApiResponse<CostResponse> response =
        fixture.createCost(null, CostCategory.LABOR, "급여", 1_000_000L, TEST_YEAR, TEST_MONTH);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 일반 멤버 토큰
    String memberToken = fixture.memberToken();

    // Act
    ApiResponse<CostResponse> response =
        fixture.createCost(
            memberToken, CostCategory.LABOR, "급여", 1_000_000L, TEST_YEAR, TEST_MONTH);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
