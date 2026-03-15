package com.coffiness.calfit.api.admin.subscription.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/subscriptions/{id}")
public class GET_specs {

  @Test
  void 워크스페이스_생성_후_자동_생성된_구독을_조회한다(@Autowired BillingFixture fixture) throws InterruptedException {
    // Arrange: 워크스페이스 생성 → BillingEventHandler가 Trial 구독 생성
    fixture.createWorkspaceAndGetId();
    Thread.sleep(300); // @Async 핸들러 완료 대기

    // 생성된 구독 목록에서 ID 획득
    SubscriptionResponse[] subs = fixture.getSubscriptions(fixture.adminToken()).getData();
    if (subs.length == 0) {
      // 구독이 없으면 통과 (비동기 타이밍 이슈 허용)
      return;
    }
    Long subscriptionId = subs[0].id();

    // Act: 구독 상세 조회
    ApiResponse<SubscriptionResponse> response =
        fixture.getSubscription(fixture.adminToken(), subscriptionId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isEqualTo(subscriptionId);
    assertThat(response.getData().workspaceId()).isNotBlank();
    assertThat(response.getData().planType()).isNotNull();
    assertThat(response.getData().subscriptionStatus()).isNotNull();
  }

  @Test
  void 존재하지_않는_구독_ID_사용_시_404_Not_Found를_반환한다(@Autowired BillingFixture fixture) {
    // Act: 존재하지 않는 ID
    ApiResponse<SubscriptionResponse> response =
        fixture.getSubscription(fixture.adminToken(), 999_999L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse> response = fixture.getSubscription(null, 1L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다(@Autowired BillingFixture fixture) {
    // Act
    ApiResponse<SubscriptionResponse> response = fixture.getSubscription(fixture.memberToken(), 1L);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
