package com.coffiness.calfit.api.admin.cost.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.CostResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/costs")
public class GET_specs {

  // 다른 테스트와 충돌을 피하기 위해 고유한 연도/월 사용
  private static final int TEST_YEAR = 1991;
  private static final int TEST_MONTH = 3;

  @Test
  void 필터_없이_요청하면_성공_응답을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostResponse[]> response = fixture.getCosts(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 카테고리_및_월_필터가_올바르게_동작한다(@Autowired BillingFixture fixture) {
    // Arrange: 같은 연도/월에 서로 다른 카테고리 비용 2건 생성
    String token = fixture.adminToken();
    fixture.createCost(token, CostCategory.LABOR, "인건비", 3_000_000L, TEST_YEAR, TEST_MONTH);
    fixture.createCost(token, CostCategory.INFRASTRUCTURE, "서버", 500_000L, TEST_YEAR, TEST_MONTH);

    // Act: LABOR만 필터
    ApiResponse<CostResponse[]> response = fixture.getCosts(token, TEST_YEAR, TEST_MONTH, "LABOR");

    // Assert: LABOR 비용만 포함, INFRASTRUCTURE 제외
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotEmpty();
    assertThat(Arrays.stream(response.getData()).allMatch(c -> c.category() == CostCategory.LABOR))
        .isTrue();
  }

  @Test
  void 페이지네이션이_올바르게_동작한다(@Autowired BillingFixture fixture) {
    // Arrange: 동일 연도/월에 비용 3건 생성 (페이지 크기 = 2)
    String token = fixture.adminToken();
    int year = 1991;
    int month = 5;
    fixture.createCost(token, CostCategory.MARKETING, "광고1", 100_000L, year, month);
    fixture.createCost(token, CostCategory.MARKETING, "광고2", 200_000L, year, month);
    fixture.createCost(token, CostCategory.MARKETING, "광고3", 300_000L, year, month);

    // Act: 첫 페이지, 크기 2
    ApiResponse<CostResponse[]> page0 = fixture.getCostsPaged(token, year, month, null, 0, 2);
    // 두 번째 페이지, 크기 2
    ApiResponse<CostResponse[]> page1 = fixture.getCostsPaged(token, year, month, null, 1, 2);

    // Assert
    assertThat(page0.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(page0.getData()).hasSize(2);
    assertThat(page1.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(page1.getData()).hasSize(1);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostResponse[]> response = fixture.getCosts(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<CostResponse[]> response = fixture.getCosts(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
