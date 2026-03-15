package com.coffiness.calfit.api.admin.cost.update;

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
@DisplayName("PUT /api/v1/admin/costs/{id}")
public class PUT_specs {

  private static final int TEST_YEAR = 1994;
  private static final int TEST_MONTH = 2;

  @Test
  void 올바르게_요청하면_수정된_비용_정보를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 먼저 비용 생성
    String token = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(token, CostCategory.LABOR, "원래 이름", 1_000_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act: 수정
    ApiResponse<CostResponse> response =
        fixture.updateCost(
            token,
            created.id(),
            CostCategory.INFRASTRUCTURE,
            "수정된 이름",
            2_000_000L,
            TEST_YEAR,
            TEST_MONTH,
            "수정 메모");

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().id()).isEqualTo(created.id());
    assertThat(response.getData().category()).isEqualTo(CostCategory.INFRASTRUCTURE);
    assertThat(response.getData().name()).isEqualTo("수정된 이름");
    assertThat(response.getData().amount()).isEqualTo(2_000_000L);
    assertThat(response.getData().memo()).isEqualTo("수정 메모");
  }

  @Test
  void 존재하지_않는_ID_사용_시_404_Not_Found를_반환한다(@Autowired BillingFixture fixture) {
    // Act: 존재하지 않는 ID
    ApiResponse<CostResponse> response =
        fixture.updateCost(
            fixture.adminToken(),
            999_999L,
            CostCategory.LABOR,
            "없는 비용",
            100_000L,
            TEST_YEAR,
            TEST_MONTH,
            null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 유효하지_않은_값_입력_시_400_Bad_Request를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 먼저 비용 생성
    String token = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(token, CostCategory.LABOR, "비용", 1_000_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // category = null (필수 필드 누락)
    Map<String, Object> rawRequest = new HashMap<>();
    rawRequest.put("name", "수정");
    rawRequest.put("amount", 2_000_000);
    rawRequest.put("year", TEST_YEAR);
    rawRequest.put("month", TEST_MONTH);

    // Act
    ApiResponse<CostResponse> response = fixture.updateCostRaw(token, created.id(), rawRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 비용 생성 후 토큰 없이 수정 시도
    String token = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(token, CostCategory.OTHER, "비용", 500_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act
    ApiResponse<CostResponse> response =
        fixture.updateCost(
            null, created.id(), CostCategory.OTHER, "수정", 600_000L, TEST_YEAR, TEST_MONTH, null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 비용 생성 후 일반 멤버로 수정 시도
    String adminToken = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(adminToken, CostCategory.MARKETING, "비용", 300_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act
    ApiResponse<CostResponse> response =
        fixture.updateCost(
            fixture.memberToken(),
            created.id(),
            CostCategory.MARKETING,
            "수정",
            400_000L,
            TEST_YEAR,
            TEST_MONTH,
            null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
