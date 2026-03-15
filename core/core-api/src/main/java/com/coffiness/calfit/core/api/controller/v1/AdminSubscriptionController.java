package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

  private final SubscriptionService subscriptionService;

  @GetMapping
  public ApiResponse<List<SubscriptionResponse>> getSubscriptions(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    SubscriptionStatus subscriptionStatus = parseStatus(status);
    List<Subscription> subs = subscriptionService.getAll(subscriptionStatus, search, page, size);
    return ApiResponse.success(subs.stream().map(SubscriptionResponse::from).toList());
  }

  @GetMapping("/{id}")
  public ApiResponse<SubscriptionResponse> getSubscription(@PathVariable Long id) {
    return ApiResponse.success(SubscriptionResponse.from(subscriptionService.getById(id)));
  }

  private SubscriptionStatus parseStatus(String status) {
    if (status == null || status.isBlank()) return null;
    try {
      return SubscriptionStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
