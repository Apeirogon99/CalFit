package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.RegisterBillingKeyRequest;
import com.coffiness.calfit.api.v1.response.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.payment.PaymentService;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;
import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.toss.TossPaymentProperties;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final TossPaymentProperties tossPaymentProperties;

  @GetMapping("/toss-config")
  public ApiResponse<TossConfigResponse> getTossConfig() {
    String workspaceId = TenantContext.getTenantId();
    String customerKey = "cust_" + workspaceId;
    return ApiResponse.success(
        new TossConfigResponse(tossPaymentProperties.clientKey(), customerKey));
  }

  @PostMapping("/billing-key")
  public ApiResponse<CardInfoResponse> registerBillingKey(
      @Valid @RequestBody RegisterBillingKeyRequest request) {
    String workspaceId = TenantContext.getTenantId();
    BillingKeyInfo info =
        paymentService.registerBillingKey(workspaceId, request.customerKey(), request.authKey());
    return ApiResponse.success(CardInfoResponse.from(info));
  }

  @PostMapping("/upgrade")
  public ApiResponse<PaymentResultResponse> upgradeToEnterprise() {
    String workspaceId = TenantContext.getTenantId();
    Optional<Payment> payment = paymentService.upgradeToEnterprise(workspaceId);
    return ApiResponse.success(payment.map(PaymentResultResponse::from).orElse(null));
  }

  @PostMapping("/downgrade")
  public ApiResponse<Void> downgradeToFree() {
    String workspaceId = TenantContext.getTenantId();
    paymentService.downgradeToFree(workspaceId);
    return ApiResponse.success(null);
  }

  @GetMapping("/card")
  public ApiResponse<CardInfoResponse> getRegisteredCard() {
    String workspaceId = TenantContext.getTenantId();
    BillingKeyInfo info = paymentService.getRegisteredCard(workspaceId);
    if (info == null) {
      return ApiResponse.success(null);
    }
    return ApiResponse.success(CardInfoResponse.from(info));
  }

  @GetMapping("/subscription")
  public ApiResponse<SubscriptionResponse> getSubscription() {
    String workspaceId = TenantContext.getTenantId();
    Subscription subscription = paymentService.getSubscription(workspaceId);
    if (subscription == null) {
      return ApiResponse.success(null);
    }
    return ApiResponse.success(SubscriptionResponse.from(subscription));
  }

  @GetMapping("/history")
  public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistory(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    String workspaceId = TenantContext.getTenantId();
    List<PaymentHistoryResponse> history =
        paymentService.getPaymentHistory(workspaceId, page, size).stream()
            .map(PaymentHistoryResponse::from)
            .toList();
    return ApiResponse.success(history);
  }
}
