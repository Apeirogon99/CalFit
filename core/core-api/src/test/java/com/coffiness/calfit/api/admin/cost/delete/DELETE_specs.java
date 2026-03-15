package com.coffiness.calfit.api.admin.cost.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.CostResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("DELETE /api/v1/admin/costs/{id}")
public class DELETE_specs {

  private static final int TEST_YEAR = 1995;
  private static final int TEST_MONTH = 9;

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 삭제할 비용 생성
    String token = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(token, CostCategory.OTHER, "삭제 테스트", 100_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act
    ApiResponse<Void> response = fixture.deleteCost(token, created.id());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 삭제된_ID로_재삭제하면_404_Not_Found를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 비용 생성 후 삭제
    String token = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(token, CostCategory.LABOR, "재삭제 테스트", 200_000L, TEST_YEAR, TEST_MONTH)
            .getData();
    fixture.deleteCost(token, created.id());

    // Act: 이미 삭제된 ID로 재삭제 시도
    ApiResponse<Void> response = fixture.deleteCost(token, created.id());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_ID를_삭제하면_404_Not_Found를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<Void> response = fixture.deleteCost(fixture.adminToken(), 999_999L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 삭제할 비용 생성
    String adminToken = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(
                adminToken, CostCategory.INFRASTRUCTURE, "비용", 50_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act: 토큰 없이 삭제 시도
    ApiResponse<Void> response = fixture.deleteCost(null, created.id());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Arrange: 삭제할 비용 생성
    String adminToken = fixture.adminToken();
    CostResponse created =
        fixture
            .createCost(adminToken, CostCategory.MARKETING, "비용", 75_000L, TEST_YEAR, TEST_MONTH)
            .getData();

    // Act: 일반 멤버로 삭제 시도
    ApiResponse<Void> response = fixture.deleteCost(fixture.memberToken(), created.id());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
