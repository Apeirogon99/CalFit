package com.coffiness.calfit.api.admin.subscription.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/subscriptions")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse[]> response = fixture.getSubscriptions(fixture.adminToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse[]> response = fixture.getSubscriptions(null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse[]> response = fixture.getSubscriptions(fixture.memberToken());

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void status가_ACTIVE이면_활성_구독만_반환한다(@Autowired BillingFixture fixture) throws InterruptedException {
    // Arrange: 워크스페이스 생성 → TRIAL 구독 자동 생성 (비동기)
    fixture.createWorkspaceAndGetId();
    Thread.sleep(300); // BillingEventHandler @Async 완료 대기

    // Act: ACTIVE 필터
    ApiResponse<SubscriptionResponse[]> response =
        fixture.getSubscriptions(fixture.adminToken(), "ACTIVE", null, 0, 50);

    // Assert: ACTIVE 구독만 있어야 함 (새로 생성된 것은 TRIAL이므로 0건 또는 기존 ACTIVE만)
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(
            Arrays.stream(response.getData())
                .allMatch(s -> s.subscriptionStatus() == SubscriptionStatus.ACTIVE))
        .isTrue();
  }

  @Test
  void search_매개변수가_매치되지_않으면_빈_리스트를_반환한다(@Autowired BillingFixture fixture) {
    // Act: 존재하지 않는 검색어
    ApiResponse<SubscriptionResponse[]> response =
        fixture.getSubscriptions(
            fixture.adminToken(), null, "NONEXISTENT_WORKSPACE_SEARCH_XYZ_12345", 0, 20);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }

  @Test
  void 페이지네이션_파라미터가_올바르게_동작한다(@Autowired BillingFixture fixture) {
    // Act: page=0, size=5로 요청
    ApiResponse<SubscriptionResponse[]> response =
        fixture.getSubscriptions(fixture.adminToken(), null, null, 0, 5);

    // Assert: 최대 5건 반환
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().length).isLessThanOrEqualTo(5);
  }

  @Test
  void status와_search를_동시에_사용해도_올바르게_동작한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse[]> response =
        fixture.getSubscriptions(fixture.adminToken(), "TRIAL", "test", 0, 20);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }
}
